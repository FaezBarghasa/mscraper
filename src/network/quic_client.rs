use crate::media::MediaError;
use bytes::{Buf, Bytes};
use http::Request;
use quinn::{ClientConfig, Endpoint};
use reqwest::Client as ReqwestClient;
use std::sync::Arc;

pub struct QuicClient {
    pub h3_client_config: ClientConfig,
    pub endpoint: Endpoint,
    pub fallback_client: ReqwestClient,
}

impl QuicClient {
    pub fn new() -> Result<Self, MediaError> {
        let _ = rustls::crypto::ring::default_provider().install_default();
        let mut root_store = rustls::RootCertStore::empty();
        root_store.extend(webpki_roots::TLS_SERVER_ROOTS.iter().cloned());

        let crypto = rustls::ClientConfig::builder()
            .with_root_certificates(root_store)
            .with_no_client_auth();

        let quic_crypto = quinn::crypto::rustls::QuicClientConfig::try_from(crypto)
            .map_err(|e| MediaError::TaggingError(e.to_string()))?;

        let client_config = ClientConfig::new(Arc::new(quic_crypto));

        let bind_addr = "[::]:0".parse().map_err(|e| MediaError::TaggingError(format!("{e}")))?;
        let mut endpoint = Endpoint::client(bind_addr)
            .map_err(|e| MediaError::TaggingError(e.to_string()))?;

        endpoint.set_default_client_config(client_config.clone());

        let fallback_client = ReqwestClient::builder()
            .timeout(std::time::Duration::from_secs(30))
            .build()
            .map_err(MediaError::NetworkError)?;

        Ok(Self {
            h3_client_config: client_config,
            endpoint,
            fallback_client,
        })
    }

    pub async fn fetch(&self, url: &str, enable_quic: bool) -> Result<Bytes, MediaError> {
        if enable_quic {
            match self.fetch_h3(url).await {
                Ok(bytes) => return Ok(bytes),
                Err(e) => {
                    eprintln!("QUIC failed: {}, falling back to HTTP/2", e);
                }
            }
        }

        self.fetch_h2(url).await
    }

    async fn fetch_h3(&self, url: &str) -> Result<Bytes, String> {
        let uri = url.parse::<http::Uri>().map_err(|e| e.to_string())?;
        let host = uri.host().ok_or_else(|| "No host in URI".to_string())?;
        let port = uri.port_u16().unwrap_or(443);

        let server_addr = format!("{}:{}", host, port)
            .parse()
            .map_err(|e: std::net::AddrParseError| e.to_string())?;

        let connection = self
            .endpoint
            .connect(server_addr, host)
            .map_err(|e| e.to_string())?
            .await
            .map_err(|e| e.to_string())?;

        let (mut driver, mut send_request) = h3::client::new(h3_quinn::Connection::new(connection))
            .await
            .map_err(|e| e.to_string())?;

        tokio::spawn(async move {
            let _ = std::future::poll_fn(|cx| driver.poll_close(cx)).await;
        });

        let req = Request::builder()
            .uri(uri)
            .body(())
            .map_err(|e| e.to_string())?;

        let mut response = send_request
            .send_request(req)
            .await
            .map_err(|e| e.to_string())?;
        response.finish().await.map_err(|e| e.to_string())?;

        let mut body_bytes = Vec::new();
        while let Some(mut chunk) = response.recv_data().await.map_err(|e| e.to_string())? {
            let chunk_bytes = chunk.copy_to_bytes(chunk.remaining());
            body_bytes.extend_from_slice(&chunk_bytes);
        }

        Ok(Bytes::from(body_bytes))
    }

    async fn fetch_h2(&self, url: &str) -> Result<Bytes, MediaError> {
        let resp = self.fallback_client.get(url).send().await?;
        let bytes = resp.bytes().await?;
        Ok(bytes)
    }
}

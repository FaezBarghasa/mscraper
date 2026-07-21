use crate::media::MediaError;
use bytes::{Buf, Bytes};
use http::Request;
use quinn::{ClientConfig, Endpoint};
use reqwest::Client as ReqwestClient;
use rustls_pki_types::{CertificateDer, ServerName};
use std::sync::Arc;

pub struct QuicClient {
    pub h3_client_config: ClientConfig,
    pub endpoint: Endpoint,
    pub fallback_client: ReqwestClient,
}

impl QuicClient {
    pub fn new() -> Result<Self, MediaError> {
        let crypto = rustls::ClientConfig::builder()
            .dangerous()
            .with_custom_certificate_verifier(Arc::new(NoCertVerifier))
            .with_no_client_auth();

        let client_config = ClientConfig::new(Arc::new(
            quinn::crypto::rustls::QuicClientConfig::try_from(crypto).unwrap(),
        ));

        let mut endpoint = Endpoint::client("[::]:0".parse().unwrap())
            .map_err(|e| MediaError::TaggingError(e.to_string()))?;

        endpoint.set_default_client_config(client_config.clone());

        let fallback_client = ReqwestClient::new();

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
        let host = uri.host().ok_or("No host")?;
        let port = uri.port_u16().unwrap_or(443);

        let connection = self
            .endpoint
            .connect(format!("{}:{}", host, port).parse().unwrap(), host)
            .map_err(|e| e.to_string())?
            .await
            .map_err(|e| e.to_string())?;

        let (mut driver, mut send_request) = h3::client::new(h3_quinn::Connection::new(connection))
            .await
            .map_err(|e| e.to_string())?;

        tokio::spawn(async move {
            let _ = driver;
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

#[derive(Debug)]
struct NoCertVerifier;
impl rustls::client::danger::ServerCertVerifier for NoCertVerifier {
    fn verify_server_cert(
        &self,
        _end_entity: &CertificateDer<'_>,
        _intermediates: &[CertificateDer<'_>],
        _server_name: &ServerName<'_>,
        _ocsp_response: &[u8],
        _now: rustls_pki_types::UnixTime,
    ) -> Result<rustls::client::danger::ServerCertVerified, rustls::Error> {
        Ok(rustls::client::danger::ServerCertVerified::assertion())
    }

    fn verify_tls12_signature(
        &self,
        _message: &[u8],
        _cert: &CertificateDer<'_>,
        _dss: &rustls::DigitallySignedStruct,
    ) -> Result<rustls::client::danger::HandshakeSignatureValid, rustls::Error> {
        Ok(rustls::client::danger::HandshakeSignatureValid::assertion())
    }

    fn verify_tls13_signature(
        &self,
        _message: &[u8],
        _cert: &CertificateDer<'_>,
        _dss: &rustls::DigitallySignedStruct,
    ) -> Result<rustls::client::danger::HandshakeSignatureValid, rustls::Error> {
        Ok(rustls::client::danger::HandshakeSignatureValid::assertion())
    }

    fn supported_verify_schemes(&self) -> Vec<rustls::SignatureScheme> {
        vec![
            rustls::SignatureScheme::RSA_PKCS1_SHA1,
            rustls::SignatureScheme::ECDSA_SHA1_Legacy,
            rustls::SignatureScheme::RSA_PKCS1_SHA256,
            rustls::SignatureScheme::ECDSA_NISTP256_SHA256,
            rustls::SignatureScheme::RSA_PKCS1_SHA384,
            rustls::SignatureScheme::ECDSA_NISTP384_SHA384,
            rustls::SignatureScheme::RSA_PKCS1_SHA512,
            rustls::SignatureScheme::ECDSA_NISTP521_SHA512,
            rustls::SignatureScheme::RSA_PSS_SHA256,
            rustls::SignatureScheme::RSA_PSS_SHA384,
            rustls::SignatureScheme::RSA_PSS_SHA512,
            rustls::SignatureScheme::ED25519,
            rustls::SignatureScheme::ED448,
        ]
    }
}

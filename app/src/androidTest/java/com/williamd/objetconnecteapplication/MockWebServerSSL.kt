package com.williamd.objetconnecteapplication

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.util.concurrent.TimeUnit

class MockWebServerSSL {
    private lateinit var mockWebServer: MockWebServer
    private val TAG = "MockWebServerSSL"
    private lateinit var heldCertificate: HeldCertificate

    fun setupMockWebServerWithSSL(context: Context) {
        try {
            // Generate a self-signed certificate for testing
            val keyPair = generateKeyPair()
            heldCertificate = HeldCertificate.Builder()
                .commonName("localhost")
                .addSubjectAlternativeName("localhost")
                .duration(365 * 24 * 60 * 60, TimeUnit.SECONDS)
                .keyPair(keyPair)
                .build()

            // Create HandshakeCertificates
            val handshakeCertificates = HandshakeCertificates.Builder()
                .heldCertificate(heldCertificate)
                .addTrustedCertificate(heldCertificate.certificate)
                .build()

            // Create SSLSocketFactory with custom settings
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
                arrayOf<KeyManager>(handshakeCertificates.keyManager),
                arrayOf<TrustManager>(handshakeCertificates.trustManager),
                null
            )

            val sslSocketFactory = sslContext.socketFactory

            // Configure MockWebServer
            mockWebServer = MockWebServer()
            mockWebServer.useHttps(sslSocketFactory, false)
            mockWebServer.start()

            Log.d(TAG, "MockWebServer started successfully with HTTPS")
            Log.d(TAG, "Server URL: ${mockWebServer.url("/")}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup MockWebServer with SSL", e)
            throw IllegalStateException("Failed to setup MockWebServer with SSL", e)
        }
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    fun getClientBuilder(): OkHttpClient.Builder {
        val handshakeCertificates = HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()
            .addTrustedCertificate(heldCertificate.certificate)
            .build()

        return OkHttpClient.Builder()
            .sslSocketFactory(
                handshakeCertificates.sslSocketFactory(),
                handshakeCertificates.trustManager
            )
            .hostnameVerifier { _, _ -> true } // Only for testing!
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
    }

    fun getMockWebServer(): MockWebServer = mockWebServer

    fun shutdown() {
        try {
            mockWebServer.shutdown()
            Log.d(TAG, "MockWebServer shutdown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down MockWebServer", e)
        }
    }
}
package com.williamd.objetconnecteapplication

import com.google.gson.internal.GsonBuildConfig
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


class HostnameVerifier() :
    HostnameVerifier {
    override fun verify(hostname: String, session: SSLSession): Boolean {
        // Autorise tous les ips local lié aux certificats
        return hostname.startsWith("192.168.") ||
                hostname.startsWith("10.") ||
                hostname.startsWith("172.16.") ||
                hostname == "localhost" ||
                hostname == "127.0.0.1"
    }
}
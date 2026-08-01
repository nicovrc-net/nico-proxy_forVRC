if ( -not (Test-Path '.\jdk-21')){
    Invoke-WebRequest -Uri https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12%2B8/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip -OutFile ./OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip
    Expand-Archive -Path ./OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip -DestinationPath ./
    Remove-Item ./OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip
    Rename-Item ./jdk-21.0.12+8 ./jdk-21
}

if ( -not (Test-Path '.\brotli.exe')){
    Invoke-WebRequest -Uri https://github.com/google/brotli/releases/download/v1.2.0/brotli-x64-windows-static.zip -OutFile ./brotli-x64-windows-static.zip
    Expand-Archive -Path ./brotli-x64-windows-static.zip -DestinationPath ./
}

./jdk-21/bin/java.exe -jar ./NicoVideoPlayForVRC-3.0-SNAPSHOT-all.jar

# SIG # Begin signature block
# MIIIpQYJKoZIhvcNAQcCoIIIljCCCJICAQExCzAJBgUrDgMCGgUAMGkGCisGAQQB
# gjcCAQSgWzBZMDQGCisGAQQBgjcCAR4wJgIDAQAABBAfzDtgWUsITrck0sYpfvNR
# AgEAAgEAAgEAAgEAAgEAMCEwCQYFKw4DAhoFAAQUMCDYTd3PEqF8vNhZ8AcoovFe
# ZSqgggUuMIIFKjCCAxKgAwIBAgIQFu7PAOinJJxLTeEeaGSG+TANBgkqhkiG9w0B
# AQsFADAsMSowKAYDVQQDDCFuaWNvdnJjLm5ldCBPVT1TZWxmLXNpZ25lZCBSb290
# Q0EwIBcNMjYwNDIwMTIxNzA0WhgPMjA5ODEyMzExNTAwMDBaMCwxKjAoBgNVBAMM
# IW5pY292cmMubmV0IE9VPVNlbGYtc2lnbmVkIFJvb3RDQTCCAiIwDQYJKoZIhvcN
# AQEBBQADggIPADCCAgoCggIBALq2vwv7CZYo6E4uRw+dE1maw/ubYxBWjMD67SZU
# b6dHv4wz2h3sNwUoCBi7pemUJ/pGfEfr8Wr/NG3uJwjw9HOJYm12Wu3hjNYVHFwq
# SiEkkYXpfAlSEOPvwuGsINLLkf5TtWGZSr/4NkvbvAVVqWsli1EOYTceOaYmXXQ+
# VXfEaNYhpOX/KmAugCDAfEuAFWZBu0jAoXIC+DvvTn1qiiSCvD4jCbbUB5I1mCIb
# l4B4mHaMpt6GrZE8G5eif5vZXa0ikt+jWuwGCyNApQhfVuDGSZtHpq78PSgbms6D
# LhOhXzwVsgno6RzGpVDeyyBzqYFURup1btajOCH7T+3SQXS6PCEdyRsNnBmRP/WS
# uhD124ElhaHn7k9HxPiaM+Om4BYQJwMvOAcRkNDsBTvLKm7gM53dd+VaMYxLfZvc
# o1cK7zqGMGCqd8V1nOstkUGYSZViaUdvP9yCkO8IqZpJv63iCHlaTTw1RT/xXosx
# RrQCKUX+D5ZKUNnT52vNs6B5W7Ijyuwxdzrfu6g3+d2vVLwBgLlepLR9NeqZgzKF
# g64QzkJ5OQkJsExBEeKeqrVuaL3jQvrI7HRhy1XAMOJlq1gefdC+bQTLEENNrATb
# 02ZhMHvmYFxj6Ce9qXDHdLZxGuVqrYtCUz6f2MFR6Nd/ifTnCDg6Cd5bn+kgwyFz
# kOphAgMBAAGjRjBEMA4GA1UdDwEB/wQEAwIHgDATBgNVHSUEDDAKBggrBgEFBQcD
# AzAdBgNVHQ4EFgQUCQFMhKmdgTdjvy01UeIx8bj4dnQwDQYJKoZIhvcNAQELBQAD
# ggIBAFhcx3r2HS99DtbsnYuVsMMt3BPE9h83dY8o0gta20T7BVgukRMoVCnKVWxT
# cKKn6umGLFptCyQgf1p5MHh+hCOUAK/fu2v6s3WNu5/HW0P/EBGmsptl+ROT8hEc
# U7p2Q9kjMcxP6w7afHm78f8PZe9IXxOf9xL7vUzqTopgOjFArUE97o4LTNTuJP4G
# 1cMJHTKy6ZFRgCpupl2ktEe6/m9HSFz61+3xkotnwZozZ1yPfz/3Knd5QqiNtE+Z
# KdyPocIjxjo6opi9uex6qMPMnXAOLms/w2rlC44bbUF+7NxIBinIS0m1nNp00z0c
# qKVcnlkeaJnfyeQsocJG+/i+EXn7cIEO8+YZJH5bsv+XexiTP8SS4QlainVwb963
# oMCZRplbrMfbWufk9cpUsW8blqJIN4+f9T5hTf8+RaqUXKDmGMPlYpEHTy1mNIke
# B27qph5Cg2R5A/EZqVJyI00Qj8/WrfH/5wJCLB9C0sKovq99iFb/6I7Mo0GNs9rt
# ZoNUZDk5aZUQLQVGRq8kXhS3O7nfF+8FsfjiAHHVf33ioG/3wnDQBZICwn273Tui
# zbZ/fyXvu7mTKsv6SsmTjGoO0ql02ufChDZgD1j8NOGmm0C5l087o4gbUW9Z8hlM
# ctVq3PhmU9UfW/Efepk3tTRRwLde7Brs3PCzw8v1iqWsq9SoMYIC4TCCAt0CAQEw
# QDAsMSowKAYDVQQDDCFuaWNvdnJjLm5ldCBPVT1TZWxmLXNpZ25lZCBSb290Q0EC
# EBbuzwDopyScS03hHmhkhvkwCQYFKw4DAhoFAKB4MBgGCisGAQQBgjcCAQwxCjAI
# oAKAAKECgAAwGQYJKoZIhvcNAQkDMQwGCisGAQQBgjcCAQQwHAYKKwYBBAGCNwIB
# CzEOMAwGCisGAQQBgjcCARUwIwYJKoZIhvcNAQkEMRYEFKxYg+9v49XCib2sE2J2
# SwtQq6VVMA0GCSqGSIb3DQEBAQUABIICAGv7uMalY1vsVDqu2WhzvYtSOpw2sEuV
# sG3x7Myl4kTKMNXu5fw7u2vcBedEXiqjy0BM9RAxCpkyvQQQusXdbk8qtclBqdYQ
# cGl4cRKkiPFQpsOGNpIQj4w7EMGa11kpjZLOuJiUH3EsNGf3hA/NtW7gZswSX3v3
# 0+3Hi/NjMl+YBEwTvIMGd0FH4qNNo5QdHyv0HKY4fTrQaJblBsbAyl66wsylgbXY
# YTkGhHZX7B73BZ/lpOLd2FOcBhOh9YtZ755vGEXBIoP6iSUY4J9DAmOgiFEcMmHL
# HPFCtU3Yj2/vTE5qQKDdVkXYou9t+9QZDiZe1d2+gsl8D4HvBVpFvx9uUY6ESuxP
# 1qoM+s9+ia/0CS5Hz8soXCTU7Ljy8OwK3Wv7tYyRqvWB8o1loMOmiL3b01YZh/9K
# rwqo3mnXysonLXH/2e0l5AqTz04NHLiC0DdCWHiMhEXenZa8LNcso4AygRqGwZa4
# VL0imGb/c+1GhZpIm+xz5dT4/1u4+bG4q6FuZ+o/vl+Q2P+KwEqXnY8SwLhVKmWj
# q7KYlWXcAhHMvoAJzrExULqdKmoCKLMSxLTX97vM52gIWLm7fnvdCPVs4ahN2bMF
# vLDqUpdQG1fYrCkb3vhJ3OcoCCTNKWXX9139R02nygZ2jMjFeQ6qsn1+3ewtx7Dd
# kwteGAWwRBdO
# SIG # End signature block

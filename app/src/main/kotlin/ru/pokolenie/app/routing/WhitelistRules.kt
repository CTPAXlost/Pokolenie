package ru.pokolenie.app.routing

/**
 * Forced whitelist domain/IP categories used when building sing-box routing.
 * Traffic matching these rules goes through the proxy; everything else is direct.
 */
object WhitelistRules {
    val geositeCategories = listOf(
        "category-ads-all",
        "google",
        "telegram",
        "discord",
        "twitter",
        "facebook",
        "instagram",
        "netflix",
        "youtube",
        "openai",
        "anthropic",
        "github",
        "cloudflare",
        "wikipedia",
        "reddit",
        "spotify",
        "tiktok",
        "geolocation-!cn"
    )

    val domainSuffixes = listOf(
        "telegram.org",
        "t.me",
        "telegram.me",
        "cdn-telegram.org",
        "discord.com",
        "discord.gg",
        "discordapp.com",
        "discordapp.net",
        "google.com",
        "googleapis.com",
        "gstatic.com",
        "youtube.com",
        "youtu.be",
        "ggpht.com",
        "googlevideo.com",
        "instagram.com",
        "cdninstagram.com",
        "facebook.com",
        "fbcdn.net",
        "twitter.com",
        "x.com",
        "twimg.com",
        "openai.com",
        "chatgpt.com",
        "anthropic.com",
        "claude.ai",
        "github.com",
        "githubusercontent.com",
        "wikipedia.org",
        "reddit.com",
        "redditstatic.com",
        "spotify.com",
        "scdn.co",
        "netflix.com",
        "nflxvideo.net",
        "medium.com",
        "linkedin.com",
        "twitch.tv",
        "cloudflare.com",
        "1.1.1.1"
    )

    val geoipCategories = listOf(
        "telegram",
        "twitter",
        "facebook",
        "cloudflare"
    )
}

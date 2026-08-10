package com.keeftalk.chat.domain.model

data class CuratedFeed(
    val title: String,
    val url: String
)

data class CuratedCategory(
    val title: String,
    val icon: String, // Icon key
    val feeds: List<CuratedFeed>
)

object CuratedFeedProvider {
    val categories = listOf(
        CuratedCategory(
            title = "World & General News",
            icon = "globe",
            feeds = listOf(
                CuratedFeed("BBC News — World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
                CuratedFeed("BBC News — Top Stories", "https://feeds.bbci.co.uk/news/rss.xml"),
                CuratedFeed("NPR — World", "https://feeds.npr.org/1004/rss.xml"),
                CuratedFeed("NPR — News", "https://feeds.npr.org/1001/rss.xml"),
                CuratedFeed("The Guardian — World", "https://www.theguardian.com/world/rss"),
                CuratedFeed("The Guardian — International", "https://www.theguardian.com/international/rss"),
                CuratedFeed("Al Jazeera — News", "https://www.aljazeera.com/xml/rss/all.xml"),
                CuratedFeed("DW — Top Stories", "https://rss.dw.com/rdf/rss-en-top"),
                CuratedFeed("France 24 — International", "https://www.france24.com/en/rss"),
                CuratedFeed("Sky News", "https://feeds.skynews.com/feeds/rss/home.xml")
            )
        ),
        CuratedCategory(
            title = "Technology",
            icon = "cpu",
            feeds = listOf(
                CuratedFeed("TechCrunch", "https://techcrunch.com/feed/"),
                CuratedFeed("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
                CuratedFeed("The Verge", "https://www.theverge.com/rss/index.xml"),
                CuratedFeed("WIRED", "https://www.wired.com/feed/rss"),
                CuratedFeed("MIT Technology Review", "https://www.technologyreview.com/feed/"),
                CuratedFeed("Engadget", "https://www.engadget.com/rss.xml"),
                CuratedFeed("CNET", "https://www.cnet.com/rss/news/"),
                CuratedFeed("Gizmodo", "https://gizmodo.com/rss"),
                CuratedFeed("ZDNET", "https://www.zdnet.com/news/rss.xml"),
                CuratedFeed("The Register", "https://www.theregister.com/headlines.atom")
            )
        ),
        CuratedCategory(
            title = "AI & Machine Learning",
            icon = "brain",
            feeds = listOf(
                CuratedFeed("OpenAI News", "https://openai.com/news/rss.xml"),
                CuratedFeed("Google AI Blog", "https://blog.google/technology/ai/rss/"),
                CuratedFeed("NVIDIA AI Blog", "https://blogs.nvidia.com/blog/category/deep-learning/feed/"),
                CuratedFeed("Hugging Face Blog", "https://huggingface.co/blog/feed.xml"),
                CuratedFeed("MIT AI", "https://news.mit.edu/topic/artificial-intelligence2/feed"),
                CuratedFeed("VentureBeat — AI", "https://venturebeat.com/category/ai/feed/"),
                CuratedFeed("MarkTechPost", "https://www.marktechpost.com/feed/"),
                CuratedFeed("Machine Learning Mastery", "https://machinelearningmastery.com/feed/"),
                CuratedFeed("Towards Data Science", "https://towardsdatascience.com/feed"),
                CuratedFeed("Import AI", "https://jack-clark.net/feed/")
            )
        ),
        CuratedCategory(
            title = "Science",
            icon = "flask",
            feeds = listOf(
                CuratedFeed("NASA", "https://www.nasa.gov/rss/dyn/breaking_news.rss"),
                CuratedFeed("ScienceDaily", "https://www.sciencedaily.com/rss/top/science.xml"),
                CuratedFeed("ScienceDaily — Space", "https://www.sciencedaily.com/rss/space_time.xml"),
                CuratedFeed("New Scientist", "https://www.newscientist.com/feed/home/"),
                CuratedFeed("Phys.org", "https://phys.org/rss-feed/"),
                CuratedFeed("Scientific American", "https://rss.sciam.com/ScientificAmerican-Global"),
                CuratedFeed("Nature", "https://www.nature.com/nature.rss"),
                CuratedFeed("Science Magazine", "https://www.science.org/rss/news_current.xml"),
                CuratedFeed("Popular Science", "https://popsci.com/feed/"),
                CuratedFeed("Live Science", "https://www.livescience.com/feeds/all")
            )
        ),
        CuratedCategory(
            title = "Space & Astronomy",
            icon = "rocket",
            feeds = listOf(
                CuratedFeed("NASA — News", "https://www.nasa.gov/rss/dyn/breaking_news.rss"),
                CuratedFeed("NASA — Image of the Day", "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss"),
                CuratedFeed("ESA", "https://www.esa.int/rssfeed/Our_Activities"),
                CuratedFeed("Space.com", "https://www.space.com/feeds/all"),
                CuratedFeed("Universe Today", "https://www.universetoday.com/feed/"),
                CuratedFeed("NASA Watch", "https://nasawatch.com/feed/"),
                CuratedFeed("Sky & Telescope", "https://skyandtelescope.org/feed/"),
                CuratedFeed("Astronomy Magazine", "https://astronomy.com/rss"),
                CuratedFeed("European Southern Observatory", "https://www.eso.org/public/rss/news/"),
                CuratedFeed("Planetary Society", "https://www.planetary.org/feeds/blog")
            )
        ),
        CuratedCategory(
            title = "Aviation & Aerospace",
            icon = "plane",
            feeds = listOf(
                CuratedFeed("Simple Flying", "https://simpleflying.com/feed/"),
                CuratedFeed("FlightGlobal", "https://www.flightglobal.com/rss"),
                CuratedFeed("Aviation Week", "https://aviationweek.com/rss.xml"),
                CuratedFeed("Airline Ratings", "https://www.airlineratings.com/feed/"),
                CuratedFeed("AeroTime", "https://www.aerotime.aero/feed"),
                CuratedFeed("The Aviationist", "https://theaviationist.com/feed/"),
                CuratedFeed("Airways Magazine", "https://airwaysmag.com/feed/"),
                CuratedFeed("AVweb", "https://www.avweb.com/feed/"),
                CuratedFeed("Air & Space Forces", "https://www.airandspaceforces.com/feed/"),
                CuratedFeed("NASA Aeronautics", "https://www.nasa.gov/rss/dyn/aeronautics.rss")
            )
        ),
        CuratedCategory(
            title = "Business & Economy",
            icon = "briefcase",
            feeds = listOf(
                CuratedFeed("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html"),
                CuratedFeed("Forbes", "https://www.forbes.com/business/feed/"),
                CuratedFeed("Entrepreneur", "https://www.entrepreneur.com/latest.rss"),
                CuratedFeed("Business Insider", "https://www.businessinsider.com/rss"),
                CuratedFeed("The Economist", "https://www.economist.com/sections/business/rss.xml"),
                CuratedFeed("Fortune", "https://fortune.com/feed/"),
                CuratedFeed("Harvard Business Review", "https://hbr.org/rss/articles"),
                CuratedFeed("Inc.", "https://www.inc.com/rss"),
                CuratedFeed("Financial Times — Companies", "https://www.ft.com/companies?format=rss"),
                CuratedFeed("MarketWatch", "https://feeds.marketwatch.com/marketwatch/topstories/")
            )
        ),
        CuratedCategory(
            title = "Finance & Investing",
            icon = "trending-up",
            feeds = listOf(
                CuratedFeed("Yahoo Finance", "https://finance.yahoo.com/rss/"),
                CuratedFeed("MarketWatch — Top Stories", "https://feeds.marketwatch.com/marketwatch/topstories/"),
                CuratedFeed("Seeking Alpha", "https://seekingalpha.com/feed.xml"),
                CuratedFeed("Investopedia", "https://www.investopedia.com/feedbuilder/feed/getfeed?feedName=rss_articles"),
                CuratedFeed("Motley Fool", "https://www.fool.com/feeds/index.aspx"),
                CuratedFeed("CoinDesk", "https://www.coindesk.com/arc/outboundfeeds/rss/"),
                CuratedFeed("Cointelegraph", "https://cointelegraph.com/rss"),
                CuratedFeed("ECB — Press Releases", "https://www.ecb.europa.eu/rss/press.html"),
                CuratedFeed("Federal Reserve", "https://www.federalreserve.gov/feeds/press_all.xml"),
                CuratedFeed("IMF", "https://www.imf.org/en/News/RSS")
            )
        ),
        CuratedCategory(
            title = "Programming & Software",
            icon = "code",
            feeds = listOf(
                CuratedFeed("GitHub Blog", "https://github.blog/feed/"),
                CuratedFeed("Stack Overflow Blog", "https://stackoverflow.blog/feed/"),
                CuratedFeed("Mozilla Hacks", "https://hacks.mozilla.org/feed/"),
                CuratedFeed("Android Developers Blog", "https://android-developers.googleblog.com/feeds/posts/default"),
                CuratedFeed("Google Developers Blog", "https://developers.googleblog.com/feeds/posts/default"),
                CuratedFeed("Kotlin Blog", "https://blog.jetbrains.com/kotlin/feed/"),
                CuratedFeed("JetBrains Blog", "https://blog.jetbrains.com/feed/"),
                CuratedFeed("Docker Blog", "https://www.docker.com/blog/feed/"),
                CuratedFeed("Kubernetes Blog", "https://kubernetes.io/feed.xml"),
                CuratedFeed("Microsoft Developer Blog", "https://devblogs.microsoft.com/feed/")
            )
        ),
        CuratedCategory(
            title = "Cybersecurity",
            icon = "shield",
            feeds = listOf(
                CuratedFeed("Krebs on Security", "https://krebsonsecurity.com/feed/"),
                CuratedFeed("The Hacker News", "https://feeds.feedburner.com/TheHackersNews"),
                CuratedFeed("BleepingComputer", "https://www.bleepingcomputer.com/feed/"),
                CuratedFeed("Dark Reading", "https://www.darkreading.com/rss.xml"),
                CuratedFeed("SecurityWeek", "https://feeds.feedburner.com/securityweek"),
                CuratedFeed("Schneier on Security", "https://www.schneier.com/feed/atom/"),
                CuratedFeed("SANS Internet Storm Center", "https://isc.sans.edu/rssfeed.xml"),
                CuratedFeed("Google Security Blog", "https://security.googleblog.com/feeds/posts/default"),
                CuratedFeed("Microsoft Security Blog", "https://www.microsoft.com/en-us/security/blog/feed/"),
                CuratedFeed("Cisco Talos", "https://blog.talosintelligence.com/feeds/posts/default")
            )
        ),
        CuratedCategory(
            title = "Gaming",
            icon = "gamepad",
            feeds = listOf(
                CuratedFeed("IGN", "https://feeds.ign.com/ignfeeds"),
                CuratedFeed("GameSpot", "https://www.gamespot.com/feeds/mashup/"),
                CuratedFeed("PC Gamer", "https://www.pcgamer.com/rss/"),
                CuratedFeed("Rock Paper Shotgun", "https://www.rockpapershotgun.com/feed"),
                CuratedFeed("Eurogamer", "https://www.eurogamer.net/feed"),
                CuratedFeed("Nintendo Life", "https://www.nintendolife.com/feeds/latest"),
                CuratedFeed("PlayStation Blog", "https://blog.playstation.com/feed/"),
                CuratedFeed("Xbox Wire", "https://news.xbox.com/en-us/feed/"),
                CuratedFeed("Kotaku", "https://kotaku.com/rss"),
                CuratedFeed("Polygon", "https://www.polygon.com/rss/index.xml")
            )
        ),
        CuratedCategory(
            title = "Cars & Motorsports",
            icon = "car",
            feeds = listOf(
                CuratedFeed("Top Gear", "https://www.topgear.com/car-news/rss"),
                CuratedFeed("Motor1", "https://www.motor1.com/rss/news/all/"),
                CuratedFeed("Car and Driver", "https://www.caranddriver.com/rss/all.xml/"),
                CuratedFeed("Road & Track", "https://www.roadandtrack.com/rss/all.xml/"),
                CuratedFeed("Jalopnik", "https://jalopnik.com/rss"),
                CuratedFeed("Autoblog", "https://www.autoblog.com/rss.xml"),
                CuratedFeed("Motorsport.com", "https://www.motorsport.com/rss/f1/news/"),
                CuratedFeed("Formula 1", "https://www.formula1.com/en/latest/all.xml"),
                CuratedFeed("The Drive", "https://www.thedrive.com/feed"),
                CuratedFeed("Hagerty", "https://www.hagerty.com/media/feed/")
            )
        ),
        CuratedCategory(
            title = "Sports",
            icon = "trophy",
            feeds = listOf(
                CuratedFeed("ESPN", "https://www.espn.com/espn/rss/news"),
                CuratedFeed("BBC Sport", "https://feeds.bbci.co.uk/sport/rss.xml"),
                CuratedFeed("Sky Sports", "https://www.skysports.com/rss/12040"),
                CuratedFeed("The Athletic", "https://theathletic.com/rss/"),
                CuratedFeed("CBS Sports", "https://www.cbssports.com/rss/headlines/"),
                CuratedFeed("Marca", "https://e00-marca.uecdn.es/rss/en/"),
                CuratedFeed("Goal.com", "https://www.goal.com/feeds/en/news"),
                CuratedFeed("Motorsport", "https://www.motorsport.com/rss/"),
                CuratedFeed("Cycling Weekly", "https://www.cyclingweekly.com/feed"),
                CuratedFeed("MMA Fighting", "https://www.mmafighting.com/rss/index.xml")
            )
        ),
        CuratedCategory(
            title = "Environment & Climate",
            icon = "leaf",
            feeds = listOf(
                CuratedFeed("NASA Climate", "https://climate.nasa.gov/news/rss.xml"),
                CuratedFeed("NOAA", "https://www.noaa.gov/rss.xml"),
                CuratedFeed("National Geographic", "https://www.nationalgeographic.com/content/natgeo/en_us/index.rss"),
                CuratedFeed("Mongabay", "https://news.mongabay.com/feed/"),
                CuratedFeed("Grist", "https://grist.org/feed/"),
                CuratedFeed("Inside Climate News", "https://insideclimatenews.org/feed/"),
                CuratedFeed("Carbon Brief", "https://www.carbonbrief.org/feed/"),
                CuratedFeed("Climate Central", "https://www.climatecentral.org/rss"),
                CuratedFeed("Yale Environment 360", "https://e360.yale.edu/feed"),
                CuratedFeed("Earth Observatory", "https://earthobservatory.nasa.gov/feeds/earthobservatory.rss")
            )
        ),
        CuratedCategory(
            title = "Education & Engineering",
            icon = "book",
            feeds = listOf(
                CuratedFeed("MIT News", "https://news.mit.edu/rss/feed"),
                CuratedFeed("Stanford News", "https://news.stanford.edu/feed/"),
                CuratedFeed("Harvard Gazette", "https://news.harvard.edu/gazette/feed/"),
                CuratedFeed("NASA Education", "https://www.nasa.gov/rss/dyn/education.rss"),
                CuratedFeed("IEEE Spectrum", "https://spectrum.ieee.org/feeds/feed.rss"),
                CuratedFeed("Engineering.com", "https://www.engineering.com/feed/"),
                CuratedFeed("Science News", "https://www.sciencenews.org/feed"),
                CuratedFeed("HowStuffWorks", "https://feeds.howstuffworks.com/HowStuffWorks-Science"),
                CuratedFeed("TED Talks", "https://www.ted.com/feeds/talks.rss"),
                CuratedFeed("The Conversation", "https://theconversation.com/global/articles.atom")
            )
        )
    )
}

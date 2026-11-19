# Thai affiliate networks, cashback services and direct-to-seller channels for a household dashboard app (Nov 2026)

All sources accessed 2026-11-19. "Official" means the programme owner's own page; "network" means an affiliate network's listing or help centre; "secondary" means a blog, aggregator or press item. Where a figure came only from a search-result snippet because the page itself would not load or sits behind a login, that is stated. Rates below are listed rates, not guarantees: nearly every source says terms are set per offer and change.

Method note: about 40 searches and fetches, 25 sources cited. Several key pages were login-walled or timed out (Involve Asia Shopee TH offer page, Cuelinks pages, Airalo guide, affiliate.shopee.co.th, Lineman Mart listing), so the official Thai commission tables for Shopee and Lazada were not obtained.

## 1. Shopee Affiliate Programme Thailand: rates, cookie, payout, API, app eligibility, deep links

### Takeaway
Shopee TH commission is a per-category table that is only visible after login, so no verified supermarket or Mall rate was found. The listed headline figures are "up to 25.90%" with a 7-day cookie, and the listed brand commission is 1.40% direct / 0.42% indirect. A Shopee Affiliate Open API (offers list, short link, conversion reports) exists. I could not confirm that it is open to Thailand or to an app publisher. Joining through a network such as Involve Asia or Admitad, which lists support for iOS and Android, is the documented route for app traffic.

### Cited Findings
- Involve Asia's Shopee TH CPS listing advertises up to 25.90% per sale, with brand commission of 1.40% for direct orders and 0.42% for indirect orders (indirect is 30% of direct). Figures come from the search-result text of the page, because the page body did not load without login. The listing does not say which categories earn the maximum — [Involve Asia, Shopee TH CPS](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program) (network; accessed 2026-11-19).
- Cookie is 7 days, per two network sources — [Involve Asia directory listing](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program) (network, snippet only) and [Admitad blog, 2025-10-13](https://www.admitad.com/idn/blog/shopee-thailand-affiliate-program-admitad) (network).
- Admitad's page says category commission amounts are not shown publicly until approval. Conversions have a 1-day processing delay. It reports automatic approval, Thailand as conversion country, support for desktop, mobile web, iOS and Android, and that deep links to products and landing pages are allowed. Payout schedule and threshold are not stated — [Admitad blog, 2025-10-13](https://www.admitad.com/idn/blog/shopee-thailand-affiliate-program-admitad) (network).
- A third-party aggregator lists 5-10% payout, with no category detail or date — [Shopper.com](https://www.shopper.com/partnerships/shopee-co-th-affiliate-program) (secondary). It conflicts with the 25.90% headline, so treat 5-10% as an unverified typical range. Another secondary source says Shopee base commission runs 2.5% to 12% by category and country — [ReachEffect blog](https://reacheffect.com/blog/shopee-affiliate-program/) (secondary, undated).
- Supermarket and Mall: no affiliate rate found. For scale only, the seller-side fee for Shopee Mall FMCG in Thailand is 11.00% commission plus 3.00% transaction fee (14.00% take rate), as of February 2025. This is what sellers pay Shopee, not what affiliates earn, and it sets an upper bound on what a Mall FMCG affiliate rate could be — [Cube Asia take-rate tracker](https://cube.asia/shopee-take-rate-tracker/) (secondary).
- Shopee Thailand started charging a service fee on partner affiliate earnings on 1 June 2025 (B.E. 2568). The fee rate is in an image that could not be read — [Shopee Thailand Help Centre](https://help.shopee.co.th/10/article/177422-%E0%B8%84%E0%B9%88%E0%B8%B2%E0%B8%9A%E0%B8%A3%E0%B8%B4%E0%B8%81%E0%B8%B2%E0%B8%A3%E0%B8%82%E0%B8%AD%E0%B8%87%E0%B9%82%E0%B8%9B%E0%B8%A3%E0%B9%81%E0%B8%81%E0%B8%A3%E0%B8%A1-Shopee-Affiliate-(Service-Fee)) (official, Thai).
- Open API: Shopee's official (Singapore help-centre) API Access article says the Affiliate Open API can retrieve Shopee, brand and product offer lists, produce a short link ("Get Short Link"), and return conversion and validated reports. It states no eligibility, application process, endpoints or regions, and Thailand is not mentioned — [Shopee Help Centre, API Access](https://help.shopee.sg/10/article/191702-API-Access) (official, Singapore).
- A third-party WordPress plugin guide lists affiliate.shopee.co.th as the Thailand sign-up page and says the App ID and key come from the "Open API" section of the dashboard — [Keywordrush Content Egg docs](https://ce-docs.keywordrush.com/modules/affiliate/shopee) (secondary). Unofficial SDKs describe a GraphQL `generateShortLink` signed with SHA256. The only endpoint found was Brazil's, not a Thai one (search snippet; secondary).
- Involve Asia offers its own Publisher API: bulk deeplink creation capped at 1,000 links per month, conversion reporting, and an offers feed that includes Shopee Commission Xtra brands. Access is by requesting an API key in the dashboard, with no formal criteria published, and the page names cashback sites among its intended users — [Involve Asia API overview](https://involve.asia/partners/api-overview/) (network).
- Shopee TH on Involve Asia: the page says Involve Asia may reject conversions that break compliance guidelines, and violators can be blocked, audited, suspended and have payment forfeited (search-result text, network). A Cuelinks mirror of the programme marks "Incentived traffic / Loyalty - Allowed" for Shopee TH and says the offer accepts Thailand traffic only. That is a search-result snippet from an aggregator and I did not open the page — [Cuelinks Shopee page](https://www.cuelinks.com/campaigns/shopee-sg-affiliate-program) (secondary, snippet only; contradicts nothing official but is unverified).
- Shopee Singapore's affiliate terms bar links on prohibited-content media such as torrent or streaming sites. They are Singapore terms and may not carry over — search result (official, Singapore), see [Shopee Help Centre, How affiliates earn](https://help.shopee.sg/10/article/125983-How-Do-Affiliates-Earn-Commissions).

### Inferences
- A household app will probably reach Shopee through a network (Involve Asia or Admitad) rather than directly. The network documents mobile apps, deeplinks and an API; Shopee's own Thai page text could not be read.
- Deep links to a product URL are documented as allowed. A deep link into the Shopee app with a search query is a different case. Nothing found says a search-results URL is or is not commissionable, so it needs a test with a real account.
- Shopee Mall FMCG take rate of 14% suggests a grocery affiliate rate well below the 25.90% headline, which is more likely a promotional or Xtra maximum. This is a guess; the real table needs a login.
- "Incentived traffic / Loyalty - Allowed" for Shopee TH would permit sharing revenue with users, but only a snippet backs it. It must be confirmed in the offer terms before any user payout is built.

### Gaps
- Per-category Shopee TH commission table (supermarket, FMCG, Mall) — behind login and not published.
- Payout minimum and schedule for Shopee TH (via Involve Asia or direct) — not found.
- Whether the Shopee Affiliate Open API is open to Thailand and to app publishers, and its Thai endpoint — not found in official text.
- Whether search-query deep links into the Shopee app are commissionable — not found.
- The Shopee service fee rate (image only).
- Official Shopee Thai affiliate terms (affiliate.shopee.co.th rendered only a title). The [affiliate.shopee.co.th homepage](https://affiliate.shopee.co.th) is the place to check.

## 2. Lazada Affiliate Programme Thailand (incl. LazMart): rates, API, app eligibility

### Takeaway
Lazada's own Thai page says up to 12% commission, a cookie of up to 30 days with last-click attribution, and that "website or application" owners can join for free. Another official Thai page says up to 13% revenue share. I found no LazMart-specific rate, no public payout minimum and no official affiliate API. The network listing for the Thai offer lists cashback, deals and incentives as disallowed media (snippet only).

### Cited Findings
- Lazada Thailand's overview page says "up to 13%" revenue share, free registration, for owners of a "website and application". It does not give cookie, payout or API details — [Lazada TH, Affiliate Program overview](https://pages.lazada.co.th/wow/i/th/corp/affiliate-overview?hybrid=1) (official, Thai).
- Lazada Thailand's "how it works" page says up to 12% commission on delivered orders, up to 30 days last-click cookie, and that app orders from your ads are also commissionable (with and without Lazada app install; details are in images). The payment-methods section is empty. No API or deep link tool is mentioned. Enquiries go to a Formstack form — [Lazada TH, How it works](https://pages.lazada.co.th/wow/i/th/corp/affiliate-howitworks?hybrid=1) (official, Thai). The two official pages disagree on the headline (12% vs 13%); the page date is unknown.
- Involve Asia's Lazada Thailand guide (2025-06-17) says rates run 2% to 10% by category and campaign, payments are monthly after a threshold it does not state, and links are made by pasting a product URL. It gives no cookie, API or LazMart detail. Creators with more than 5,000 Instagram or 50,000 TikTok followers can apply to higher-paying Lazada Talent campaigns — [Involve Asia, Lazada Thailand guide](https://involve.asia/blog/lazada-thailand-affiliate-program/) (network, 2025-06-17).
- Cuelinks's Lazada TH listing, in search-result text: "Disallowed Media: Deals, Coupons, Cashback, Reward Points, Incentives, Charity". Malaysia's listing marks Cashback and Incentivised traffic/Loyalty "Forbidden", while a Philippines app listing marks incentivised traffic "Allowed". I did not open the page — [Cuelinks, Lazada TH](https://www.cuelinks.com/campaigns/lazada-th-affiliate-program) (secondary, snippet only; the listing is a network mirror and the markets conflict).
- Lazada's general affiliate terms were found only for the Philippines and Singapore. They cover branding, advertising material, trademark use, crawling and a ban on brand-keyword SEM. No cashback clause appeared in the excerpts — [Lazada PH affiliate terms](https://pages.lazada.com.ph/wow/i/ph/corp/affiliate-terms) (official, Philippines). The Thai terms page returned no usable excerpt.
- Lazada joined YouTube Shopping's Thailand affiliate programme in November 2025 and Meta's Facebook affiliate partnerships in July 2026. These are creator channels, not app routes — [Bangkok Post, Magzter listing](https://www.magzter.com/hi/stories/newspaper/Bangkok-Post/LAZADA-JOINS-YOUTUBE-SCHEME) (secondary; the dates are as summarised by the search tool and not checked against the article).

### Inferences
- Lazada is the one platform whose official Thai wording names "application" owners as eligible, so it is the clearer fit for an app publisher than Shopee.
- If the Thai network listing really bans cashback, rewards and incentives, then sharing revenue with users on Lazada would breach its terms. The conflicting Philippines listing shows the rule varies by market, so it must be checked in the Thai offer terms.
- Without an official API, per-user product links would need network link tools (Involve Asia API) or manual deeplinks.

### Gaps
- Lazada Open Platform affiliate API: no official documentation found, so existence and Thai availability are unconfirmed.
- LazMart or grocery category rate — not found.
- Payout minimum, validation window and the official Thai terms on cashback and incentivised traffic — not found.
- Whether Lazada's own affiliate portal and the Involve Asia route differ in rate.

## 3. Affiliate networks in Thailand: merchants, rates, and whether app publishers can join

### Takeaway
Involve Asia is the most documented network for apps: it has an API aimed at app and cashback publishers, and its help centre sets rules per offer rather than per publisher type. AccessTrade Thailand is large, runs campaign-by-campaign rules and has app-install campaigns, but no public publisher terms or rates were found. No Thai Lotus's, Big C, Makro, Tops, Grab or 7-Eleven affiliate offer was found on either network.

### Cited Findings
- Involve Asia: advertisers set which promotion methods are allowed per offer. Incentivised traffic is "only allowed if clearly stated in offer". Cashback & Loyalty, Push Notification (browser and mobile app) and Coupon codes are listed as methods with no extra restriction. There is no category for apps. "If your chosen method isn't listed as 'Allowed,' do not use it." — [Involve Asia Help Centre, Available promotion methods](https://helpcentre.involve.asia/portal/en/kb/articles/available-promotion-methods) (network).
- Involve Asia's violation guide lists incentivised traffic where not permitted, unapproved brand bidding and click spamming as violations — [Involve Asia Help Centre, Violation management](https://helpcentre.involve.asia/portal/en/kb/articles/violation-management-advertisers-involve-asia) (network, from search text).
- Involve Asia's API page names app, cashback and content publishers as intended users and says the API can build custom affiliate apps — [Involve Asia API overview](https://involve.asia/partners/api-overview/) (network).
- Klook on Involve Asia (rates labelled April-June 2026): eSIM 20%; tours, activities and hotels 6.5%; food, attractions, transport, car rental, insurance 5%; gift card and special activities 2%. Cookie 30 days (7 for hotels and car rental). Deep links via the dashboard. Thailand is one of 12 promoted countries, worldwide traffic accepted, and no payout minimum is stated. The page metadata shows 2024-04-29, so confirm before use — [Involve Asia, Klook guide](https://involve.asia/blog/klook-affiliate-program/) (network).
- Agoda: a June 2026 third-party teardown puts commission at about 4% to 7% by monthly volume, with deep links, widgets and an API, and a cookie "about 30 days" described as typical. Another source says 1 day. Payout thresholds conflict ($50 vs $200). It could not be tied to an official page — [Track360 teardown, 2026-06-10](https://track360.io/blog/agoda-affiliate-program-operator-teardown-2026) (secondary); contradicted on cookie and payout by the Agoda-program roundup in the search results, e.g. [Travelpayouts Agoda page](https://travelpayouts.com/en/offers/agoda-affiliate-program) (network). Agoda commission applies to accommodation only, per Travelpayouts. The official sign-up is partners.agoda.com, which I did not load.
- LINE MAN Mart TH is listed on Cuelinks as CPS at 14% for new customers and 3.5% for existing, 65-day payment terms, but marked "paused by the advertiser" — [Cuelinks, Lineman Mart TH](https://www.cuelinks.com/campaigns/lineman-mart-th-cps-affiliate-program) (secondary, snippet only; the page timed out when fetched).
- Grocers: Involve Asia's Lotus's programme and case study are for Malaysia, not Thailand — [Involve Asia, Lotus's affiliate programme](https://involve.asia/blog/lotuss-affiliate-program/) (network). No Thai Lotus's, Big C, Tops, Makro or 7-Eleven offer was found. Makro PRO leads Thai grocery e-commerce with 39.5% share, Lotus's SMART App 19.5%, in a 64 billion baht market (2024) — [Bangkok Post, Makro PRO](https://bangkokpost.com/thailand/pr/2979876/makro-pro-crowned-thailands-top-grocery-e-commerce-platform-amid-small-business-boom-) (press release).
- AccessTrade Thailand: "Thailand's largest affiliate sub-network" with 60,000 local publishers per Rakuten Advertising's blog. Each campaign sets its own payout model (CPS, CPL, CPI), accepted traffic and rules. ACCESSTRADE runs app-install campaigns and integrates with the Airbridge attribution tool for installs and in-app purchases. A TikTok Shop campaign launched with no follower minimum. No official publisher terms or rate table were found, and the widely quoted 2% figure comes from a third-party directory — [Airbridge integration post](https://www.airbridge.io/blog/accesstrade-integration) (secondary), [Rakuten Advertising blog](https://blog.rakutenadvertising.com/?p=37574) (secondary).
- Other networks that appear in results: Admitad (Shopee TH offer, see section 1), Ecomobi/Optimise (Agoda 4.8% advertised) and Cuelinks (mirror pages). None of their Thai terms for app traffic were opened.
- Airalo: its own affiliate guide says commission starts at 10% per sale on tracked links, tracked in Impact, with mobile app conversions supported through an Impact-Adjust integration. Payment is by the end of the following month. A separate REST API partner model exists and is distinct from the affiliate model. The guide page returned HTTP 403 on fetch, so these points come from search-result text — [Airalo affiliate guide](https://www.airalo.com/ru/blog/the-airalo-affiliate-program-everything-you-need-to-know) (official, snippet); cookie of 30 days is from [Travelpayouts, Airalo](https://www.travelpayouts.com/en/offers/airalo-partner-program/) (network).

### Inferences
- A Thai app can plausibly be an Involve Asia publisher via its API, as the network explicitly names cashback and app publishers. Approval and per-offer traffic rules still decide each merchant.
- Thai grocery affiliate options look thin. The big grocers seem to sell through their own apps with no public affiliate offer, so grocery revenue would mostly come via Shopee or Lazada marketplaces or LINE MAN Mart if it returns.
- Travel (Klook, Agoda, Trip.com, Traveloka) is where documented rates are, and eSIM at 10-20% is the highest-percentage lead among the widget fits.

### Gaps
- Involve Asia's general publisher T&Cs on app traffic and on cashback — not found (only per-offer rules and a help article).
- AccessTrade Thailand merchant directory, rates and traffic rules — behind login.
- Trip.com, Traveloka, Grab, 7-Eleven, Big C, Tops, Makro affiliate terms in Thailand — no sources found.
- Official Agoda partner terms and Klook's own affiliate portal (affiliate.klook.com) — not loaded.

## 4. Cashback and rewards in Thailand (ShopBack TH and similar), including partner or white-label APIs

### Takeaway
ShopBack TH is a large cashback service (reported 1,300+ merchants and 4 million users, undated) with Lazada, Grab, Agoda, Trip.com and Klook among its merchants. I found no public ShopBack partner or white-label API for an app to pass cashback through. White-label cashback APIs exist from other vendors (FlipGive, Wildfire), but none was found targeting Thailand.

### Cited Findings
- ShopBack launched in Thailand in July 2017 with over 100 merchants including Uber, Lazada, Konvy and Grab — [Inside Retail Asia, 2017-07-19](https://insideretail.asia/2017/07/19/shopback-thailand-launches-with-100-merchants/) (press, dated 2017).
- A 2026 Nation Thailand piece describes ShopBack giving Thai users 4X cashback with Trip.com, and a campaign listing Agoda, Trip.com, Expedia and Booking.com — [Nation Thailand](https://www.nationthailand.com/business/30342787) (press; the 1,300+ merchants and 4 million users figure appears in the search summary of this and related results and is not independently dated).
- ShopBack integrates with merchants and travel affiliates by API: Agoda's Rocket Travel built a post-booking API for ShopBack, and Branch has a ShopBack attribution guide. Neither is a public partner API for third-party apps — [Rocket Travel case study](https://www.rockettravel.com/case-studies/shopback) (secondary).
- Involve Asia's API is documented as the channel for cashback and app publishers, so a cashback feature could be built directly on a network rather than through ShopBack — [Involve Asia API overview](https://involve.asia/partners/api-overview/) (network).
- Platform terms that matter for passing cashback on: Lazada TH is listed as disallowing cashback and incentives (snippet, section 2); Shopee TH is listed as allowing incentivised traffic/loyalty (snippet, section 1); Wise forbids business use of its referral link and sharing it beyond family and friends (section 6) — [Wise referral terms, effective 2026-05-05](https://wise.com/gb/legal/referral-terms) (official, UK).
- White-label cashback APIs exist elsewhere: FlipGive offers a Rewards API and Wildfire a white-label cashback platform — [FlipGive Rewards API docs](https://docs.flipgive.com/docs/rewards-api/overview) (vendor docs; not Thai).

### Inferences
- Revenue sharing with users is possible in principle only on offers that allow incentivised traffic. Shopee TH is listed as allowing it (unconfirmed); Lazada TH as barring it (unconfirmed); Wise bars it.
- A "pass-through cashback" feature has no ready Thai partner API, so it would have to be built as a self-run rebate on top of network conversion data, with per-offer permission.

### Gaps
- Any ShopBack developer, partner or white-label API — none found.
- Other Thai cashback services (e.g. Cashbac, Shopee/Lazada loyalty) were not researched.
- Current ShopBack TH merchant count and rates — only undated or old figures.

## 5. Direct sellers and LINE: ordering, share routes, and a seller-side business model

### Takeaway
LINE gives an app a clean route to hand a shopping list to a shop's OA: the `line.me/R/oaMessage/{id}/?{text}` link opens the shop's chat with the list prefilled, and the user presses send. There is no documented way for an app to send into a shop's chat on the user's behalf, or to push a message to a shop that isn't a friend. LIFF `shareTargetPicker` lets the user pick any recipient but cannot pre-select one. I found no Thai precedent or pricing for a lead-fee or listing-fee model.

### Cited Findings
- LINE's URL scheme: the path `line.me/R/oaMessage/{percent-encoded LINE ID}/?{text}` on the https scheme opens the OA chat and enters the text in the input field. The docs do not say it is sent automatically. the `line.me/R/share?text=` path opens a share screen where the user picks recipients. `ti/p/{id}` opens the OA profile. Supported on iOS and Android LINE only, not on desktop LINE. The LINE ID is the Basic or Premium ID — [LINE Developers, URL scheme](https://developers.line.biz/en/docs/messaging-api/using-line-url-scheme) (official).
- `liff.shareTargetPicker()` takes up to 5 messages (text, image, Flex, template and others) and lets the user choose recipients among friends, groups and chats. Recipient pre-selection is not supported. It needs the picker enabled in the console, and in an external browser a single-sign-on session. It does not report how many recipients were chosen — [LIFF API reference](https://developers.line.biz/en/reference/liff/) (official).
- `liff.sendMessages()` works only when the LIFF app was launched from a 1:1, group or multi-person chat inside the LIFF browser, with the `chat_message.write` scope — [LIFF API reference](https://developers.line.biz/en/reference/liff/) (official).
- LIFF apps run in an external browser with limits (for example no `scanCode()`) — [LIFF overview](https://developers.line.biz/en/docs/liff/overview/) (official).
- Messaging API cost and mechanics: reply messages do not count toward the plan, push, multicast and broadcast are counted per recipient. The page does not show Thai plan prices — [LINE Developers, Messaging API pricing](https://developers.line.biz/en/docs/messaging-api/pricing/) (official). A third-party table reports Thai plans of 0 baht (300 messages), 1,280 baht (15,000) and 1,780 baht (35,000), excluding VAT, checked September 2026 — [YesPress, LINE Thailand](https://yespress.io/line-company-thailand) (secondary).
- From 1 February 2027, LINE Thailand will give every OA 25,000 free Reply API messages a month and charge above that through a designated agency, announced 2026-10-30 — [Relevant Audience](https://www.relevantaudience.com/social-media-marketing/line-reply-api-25000-free-messages-agency-global-platform/) (secondary, press summary).
- LINE Thailand reports 56 million users and 12 billion OA conversations in 2025. LINE MyShop is positioned as chat commerce for merchants, and LINE MINI Apps opened to developers in March 2026, with 1,000+ apps. These are LINE's own disclosures, with no merchant fees given — [Relevant Audience, 2026-08-12](https://www.relevantaudience.com/digital-marketing-en/line-thailand-business-solutions-2026/) (secondary, vendor-reported).
- PromptPay via a third-party gateway (Omise) costs 1.65% per transaction plus VAT. It is shown for scale only and does not describe any wet-market vendor's arrangement — [Omise Thailand pricing](https://www.omise.co/pricing/thailand) (vendor).

### Inferences
- A "send list to shop" button can be built with the oaMessage link alone: no seller integration, no API key and no cost. It is one tap short of automatic, and it works only where the shop has a LINE OA with a known Basic ID.
- A seller-side business model would sit outside LINE's own rails: a lead fee or subscription charged to the shop by the app. Messages from shop to customer would use the shop's own OA quota, which the shop pays for.
- Seller-side revenue is the only channel here that does not depend on a platform's incentive rules, which matters for sharing revenue with users.

### Gaps
- LINE MyShop fees, availability and PromptPay terms — not found in sources.
- Thai precedent for lead-fee or listing-fee models among wet-market or small grocers — none found. (The related Grab and LINE MAN merchant commission figures I saw are restaurant GP rates, not relevant.)
- Whether wet-market vendors commonly use LINE OA versus personal LINE accounts — no data found; an oaMessage link does not work for personal accounts.
- A fuller treatment of LINE Messaging API push to non-friends (not allowed to non-friends in LINE's model; the pricing page is silent on it).

## 6. Widget-specific affiliate fits (calendar, world clock, air quality, Wan Phra, rain, favorites, market data)

### Takeaway
Documented programmes exist for travel (Klook, Agoda), eSIM (Airalo, Klook eSIM) and streaming (Disney+ Hotstar), with Thai commission figures only for Klook and a regional range for Agoda. Wise's referral programme is personal-use only and bars commercial promotion. I found nothing citable for flower or merit-offering channels, air-quality products, brokerage introducers or a rain/food-delivery programme.

### Cited Findings
**Calendar long weekends and travel**
- Klook (Thailand promoted): 6.5% on hotels, tours and activities, 5% on transport and car rental, 30-day cookie (7 days for hotels). See section 3 — [Involve Asia, Klook guide](https://involve.asia/blog/klook-affiliate-program/) (network).
- Agoda: about 4% to 7% by monthly volume per a June 2026 third-party teardown, hotels only per Travelpayouts. See section 3 — [Track360](https://track360.io/blog/agoda-affiliate-program-operator-teardown-2026) (secondary).
- Trip.com and Traveloka: no affiliate terms found. ShopBack lists Trip.com cashback (section 4). Bangkok Airways, AirAsia and HopeGoo appear as Involve Asia offers; their rates were not opened — [Involve Asia, Bangkok Airways offer](https://app.involve.asia/directory/bangkok-airways-affiliate-program) (network; it bars brand keywords in paid search).

**World clock: eSIM, flights, money transfer**
- Airalo: from 10% per sale, tracked in Impact, mobile app tracking via Adjust, 30-day cookie, payment by end of next month — [Airalo guide](https://www.airalo.com/ru/blog/the-airalo-affiliate-program-everything-you-need-to-know) (official, snippet); [Travelpayouts](https://www.travelpayouts.com/en/offers/airalo-partner-program/) (network). Klook eSIM is listed at 20% (section 3).
- Wise referral: for individual personal use only, not commercial use. Sharing the link beyond family and friends, posting it on voucher or affiliate sites, and paid search are all prohibited. The programme may move commercial referrers to the Affiliate Partner Program, which has separate terms — [Wise referral terms, 2026-05-05](https://wise.com/gb/legal/referral-terms) (official, UK). A Thai-specific reward was not confirmed; the only Thai figure (THB 100) came from a domain that is not Wise's — [Wise help: how the invite programme works](https://wise.com/help/articles/2487043/how-does-the-invite-program-work) (official; rewards vary by region).

**Air quality: masks, purifiers, filters**
- No dedicated programme found. Purifier and filter sales would run through Shopee or Lazada categories (sections 1 and 2), whose category rates are unknown.

**Wan Phra and Buddhist days: flowers and offering sets**
- Shopee and Lazada both publish editorial content on worship flowers and carry flower listings, with no flower-specific affiliate rate or programme found — [Lazada blog, worship flowers](https://www.lazada.co.th/blog/9-ดอกไม้ไหว้พระ/) (marketplace content), [Shopee blog](https://shopee.co.th/blog/flowers-worship-buddha-image) (marketplace content). No source covered monk-offering sets (sangkhathan) online, or local flower-delivery affiliate options.

**Rain: food-delivery promos**
- LINE MAN Mart TH was a Cuelinks CPS offer at 14% new / 3.5% existing customers, currently paused (section 3). No Grab Thailand affiliate programme was found; Grab pages found are merchant and driver referral schemes.

**Favorites: streaming referral**
- Disney+ Hotstar offer listed for Thailand, Indonesia and Malaysia (listed 2025-06-06) with a 60-day hold, amount not shown — [Indoleads](https://indoleads.com/?p=26954) (network, snippet). Viu has a Thailand Cuelinks campaign, paused. Netflix's affiliate programme has ended, per a 2024 review — [Authority Hacker](https://www.authorityhacker.com/netflix-affiliate-program/) (secondary, 2024). No WeTV programme found.

**Market data: Thai brokerage introducers and licensing**
- No Thai brokerage referral programme with rates was found. The Thai SEC enforces against unlicensed investment advice and portfolio management, and warns firms that some activities may be regulated securities, derivatives or digital-asset business. None of the results addressed paid introducing or referral fees directly — [InfoQuest, SEC refers unlicensed adviser](https://www.infoquest.co.th/?p=368549) (secondary, Thai press).

### Inferences
- Highest-value and best-documented widget fits: calendar to Klook and Agoda, world clock to eSIM. Both have clear user intent and published rates.
- Wise is a poor fit for an app: its terms bar commercial promotion of the referral link, and its affiliate route needs a separate agreement.
- Brokerage is a regulatory risk, not a documented revenue stream. Showing a market-data widget with an unlicensed referral fee might fall under the SEC's licensing concerns, but the sources found do not say so. Take legal advice before any such link.
- Air quality, Wan Phra and rain have no dedicated programmes; they would reuse Shopee, Lazada or delivery-app links already covered above.

### Gaps
- Trip.com, Traveloka affiliate terms in Thailand.
- Official Thai SEC rule on introducers or referral commissions for brokers — not found (a Thai-language search returned only enforcement cases).
- Flower, offering-set and temple-delivery channels, and any affiliate programme for them.
- Thai purifier or mask brand affiliate programmes (Xiaomi, Sharp).
- Disney+, Viu and WeTV current commission amounts.

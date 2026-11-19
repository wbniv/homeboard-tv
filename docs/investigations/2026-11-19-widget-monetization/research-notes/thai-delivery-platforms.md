# Thai grocery and delivery platforms: integration and monetization surfaces for a third-party shopping-list app

All sources accessed 2026-11-19. Confidence labels: **CONFIRMED** = official/primary page read directly; **REPORTED** = press or third-party page; **UNVERIFIED** = search-result summary only (page not read in full) or inference. Items marked "not found" mean the searches run did not surface anything. They do not prove absence. Only about 25 searches/fetches were run, so the deep-link questions in particular are open: I could not inspect any app's AndroidManifest.

Scope note: Thailand only. Several searches surfaced Malaysian, Singaporean or US programs (Lotus's Malaysia, Instacart); these are flagged and must not be read as Thai.

## 1. Grab Thailand (GrabMart, GrabFood, GrabExpress, GrabAds): API, deep links, affiliate

### Takeaway
Grab's developer surface that I could evidence is merchant-side (GrabFood partner API, OAuth2 client-credentials, orders/menus/store status); I found no consumer-facing "send a cart" API, no documented GrabMart deep-link scheme, and no Thai affiliate or referral programme. The developer portal is JavaScript-rendered, so its GrabMart pages could not be read; treat GrabMart API scope as unconfirmed.

### Cited Findings
- developer.grab.com loads as an empty shell to a plain HTTP fetch (title "Grab Developer" only), and `/docs/grabmart/` returned the same, so no official portal content could be read. CONFIRMED (fetch result, negative) — [Grab Developer](https://developer.grab.com/), accessed 2026-11-19; [GrabMart docs path](https://developer.grab.com/docs/grabmart/), accessed 2026-11-19
- Grab's official GrabFood SDK (Java) is a client for the GrabFood **partner API** (API version 1.1.3). Endpoints are order handling (accept/reject/edit/cancel/refund/mark ready), menus, store hours/status, campaigns, dine-in vouchers and POS order sync, which indicates merchant/POS integrators, not consumer apps. README states no audience explicitly (my reading of the endpoint list). CONFIRMED for contents, inference for audience — [grab/grabfood-api-sdk-java](https://github.com/grab/grabfood-api-sdk-java), accessed 2026-11-19
- Auth for that API is OAuth2 client credentials: POST `client_id`, `client_secret`, `grant_type` to `/grabid/v1/oauth2/token`, scope `food.partner_api`, then Bearer token. Base URI host is partner-api.grab.com under path /grabfood (from a search-result snippet of the Go SDK README). CONFIRMED (Java README); base URI UNVERIFIED — [grab/grabfood-api-sdk-java](https://github.com/grab/grabfood-api-sdk-java), accessed 2026-11-19; [grab/grabfood-api-sdk-go](https://github.com/grab/grabfood-api-sdk-go), accessed 2026-11-19 (snippet only)
- No GrabMart-specific SDK appeared in search results. A third-party directory describes a GrabMart API for grocery/convenience merchants (catalog, inventory) requiring a developer account and approved OAuth credentials; this is aggregator text, not official. UNVERIFIED — [apis.io Grab provider page](https://apis.io/providers/grab/), accessed 2026-11-19
- Grab Thailand's enterprise page lists: Grab for Business, GrabAds (online-to-offline advertising on Grab's user base), GrabGifts (vouchers), GrabExpress for business (package booking), GrabMaps, and a footer link to express.grab.com. It lists a Developer Portal link under Quick Links but describes no API integration, deep links, affiliate or delivery API for consumer apps. CONFIRMED (as far as the page goes; summarised by fetch tool) — [Grab TH enterprise](https://www.grab.com/th/campaign/enterprise/), accessed 2026-11-19
- GrabMart has no separate app; it is the "Mart" tab inside the Grab app (Grab Malaysia/Myanmar launch-era pages). CONFIRMED but regional/old (2020) — [Grab MY: Introducing GrabMart service type](https://www.grab.com/my/blog/driver/introducing-new-grabmart-service-type/), accessed 2026-11-19 (snippet only)
- Deep links: searches for a documented `grab://` scheme or any GrabMart search/store/cart URL found nothing; no Grab developer or partner document describing one surfaced. NOT FOUND — see Gaps.
- Affiliate/referral: no Grab Thailand affiliate or publisher programme found on Involve Asia, AccessTrade, ShopBack or Grab's own Thai pages. The only "affiliate" hit is the legal definition (group company) in Grab's GrabMart merchant contract, last modified 7 Feb 2023. NOT FOUND — [GrabMart merchant contract TH](https://www.grab.com/th/en/terms-policies/general-terms-and-conditions-of-grabmart-merchant-contract/), accessed 2026-11-19 (search-result summary); [Involve Asia directory example listing for Thai campaigns](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program), accessed 2026-11-19
- Grab Thailand does run merchant onboarding for GrabMart (via GrabMerchant app) and consumer promotions with partners (e.g. ttb cards; Tops/The 1 points on GrabMart to 15 Dec 2025), i.e. partner deals are bilateral commercial arrangements, not self-serve programmes. REPORTED — [Tops/Central Retail: partners with The 1 and GrabMart](https://corporate.tops.co.th/news/tops-under-central-retail-partners-with-the-1-and-grabmart-to-expand-into-digital-quick-commerce-market-integrating-a-loyalty-program-to-earn-the-1-points-on-grabmart-transforming-the-shopp/), accessed 2026-11-19 (search-result summary)
- Retail partners on GrabMart in Thailand include Big C and Gourmet Market (undated Bangkok Post item). REPORTED, undated — [Bangkok Post 2398308](https://www.bangkokpost.com/business/general/2398308), accessed 2026-11-19 (search-result summary)

### Inferences
- A third-party app cannot, on current evidence, add to a GrabMart cart or earn from referrals via a public programme. The realistic Grab route is a bilateral business-development deal; no precedent for an app-level referral fee with Grab was found.
- Even if GrabMart accepted web or app links, nothing indicates it would pay on them.

### Gaps
- Real content of developer.grab.com (GrabMart/GrabExpress API scope, whether any consumer-facing or "Mini App/Superapp" integration exists). Needs a headless browser or manual read.
- Whether Grab's Android app registers a documented custom scheme or App Link for Mart search/cart. Would require pulling the APK manifest (`aapt dump xmltree`) or on-device `adb shell dumpsys package`.
- Whether Grab runs a Thai referral programme outside the public networks checked (e.g. direct invite-only, or GrabAds creator tools).

## 2. LINE MAN Wongnai (LINE MAN Mart): partner APIs, deep links, affiliate

### Takeaway
LINE MAN Wongnai offers advertising/coupon "marketing solutions" and merchant tooling (Wongnai POS, FoodStory POS, Merchant App), but I found no public developer API, no documented deep-link scheme, and no affiliate programme. Commercial routes run through a partnership enquiry form.

### Cited Findings
- LMWN "Be Our Partner" page offers: marketing solutions, in-app media placement, advertorial/social media, and "Partnership Opportunity"; contact `partnership@lmwn.com`. B2C options include in-app banner and coupon codes (discount codes for LINE MAN FOOD, MART, MESSENGER or RIDE). The page states 10M+ monthly active users, 700K+ merchant users, 55K+ POS users. CONFIRMED — [LMWN: Be Our Partner](https://lmwn.com/be-our-partner-en/), accessed 2026-11-19
- Merchant products: Wongnai POS (receives LINE MAN delivery orders alongside dine-in), FoodStory POS (acquired), JERA Cloud (beauty/health POS, first non-restaurant POS). No developer docs or API reference surfaced. REPORTED — [Wongnai POS](https://www.wongnai.com/pos/), accessed 2026-11-19 (search-result summary); [LMWN JERA Cloud acquisition](https://lmwn.com/jera-cloud-acquisition-en/), accessed 2026-11-19 (listed in results, not read)
- LINE MAN MART has partnered with Watsons to expand Health & Beauty. REPORTED — [Thailand Edition](https://thailandedition.com/i/line-man-mart-watsons-partner-expand-beauty-health-00a16992dab0b21546faded1), accessed 2026-11-19 (search-result summary)
- Deep links: no `lineman://` or web-link scheme documented. LY Corporation's LINE docs say no URL scheme exists to launch native apps other than LINE, which is about LINE's own scheme and does not address LINE MAN. NOT FOUND — [LINE URL scheme docs](https://developers.line.biz/en/docs/line-login/using-line-url-scheme/), accessed 2026-11-19 (search-result summary)
- Affiliate: no LINE MAN affiliate or referral programme found. NOT FOUND.
- Market position: Momentum Works data reported in 2026 puts Grab 47% and LINE MAN Wongnai 41% of Thai food delivery GMV in 2025, ShopeeFood 10%. LINE MAN Wongnai is reported to oppose price controls and to warn about a 2027 IPO. REPORTED (search-result summaries) — [The Phuket News: Grab/LINE MAN duopoly](https://www.thephuketnews.com/grab-line-man-duopoly-tightens-its-grip-99167.php), accessed 2026-11-19 (HTTP 403 on direct fetch, summary from search); [The Nation: LINE MAN opposes TCCT price controls](https://www.nationthailand.com/business/corporate/40073508), accessed 2026-11-19 (snippet)

### Inferences
- LINE MAN's only third-party-facing commercial surface found is paid advertising/coupons for brands and bespoke partnerships. For a list app the practical route is a partnership email (`partnership@lmwn.com`), not self-serve integration.

### Gaps
- Any private/partner API (e.g. for POS vendors) and its terms.
- Whether LINE MAN supports Android App Links for store/search URLs (manifest not inspected).

## 3. foodpanda / pandamart in Thailand

### Takeaway
foodpanda ceased operations in Thailand on 23 May 2025 (announced 23 April 2025), covering food and grocery delivery; it is not a live target in Nov 2026. No sign of a return was found.

### Cited Findings
- Delivery Hero announced foodpanda would cease operations in Thailand on 23 May 2025 after 13 years, ending "food and grocery delivery"; the article does not name pandamart specifically. REPORTED (press) — [Thailand Business News](https://www.thailand-business-news.com/companies/212648-foodpanda-to-cease-operations-in-thailand), accessed 2026-11-19; also [Khaosod English, 2025-04-23](https://www.khaosodenglish.com/news/business/2025/04/23/foodpanda-exits-thailands-food-delivery-market-after-years-of-losses/), accessed 2026-11-19 (HTTP 403 on direct fetch; headline/summary from search results)
- Cited reason: Delivery Hero's "optimise geostrategy"; reportedly never profitable in 13 years. REPORTED — [MarkTech APAC](https://marketech-apac.com/foodpanda-to-exit-thailand-market-citing-geostrategy-optimisation/), accessed 2026-11-19 (search-result summary)
- No pandamart-Thailand news surfaced; Singapore pandamart XL expansion (Jan 2026) and a Taiwan pandamart closure are unrelated to Thailand. REPORTED — [MarkTech APAC: pandamart XL Singapore](https://marketech-apac.com/foodpanda-expands-quick-commerce-grocery-operations-with-pandamart-xl-stores-in-singapore/), accessed 2026-11-19 (snippet)
- APIs, deep links, affiliates: moot; the service is closed. NOT APPLICABLE.

### Inferences
- Skip foodpanda/pandamart. Any older foodpanda affiliate or deep-link documentation is obsolete for Thailand.

### Gaps
- Whether anyone acquired the Thai foodpanda brand or customer base after May 2025; not found.

## 4. ShopeeFood, Robinhood, HappyFresh

### Takeaway
ShopeeFood is operating in Thailand in 2026 with no public consumer integration programme found. Robinhood is still operating but its ownership is in flux: a SKY ICT/TTA venture is reported (11 Nov 2026) to take over IP and operations in December 2026. HappyFresh left Thailand in September 2022.

### Cited Findings
- ShopeeFood: still active in Thailand; 2026 items show growth (CB Insights: Thailand fastest growth at 22%), a five-year anniversary campaign in October (Thairath), and inclusion in a June 2026 government co-payment scheme alongside GrabFood and Robinhood. REPORTED (search-result summaries) — [CB Insights ShopeeFood](https://www.cbinsights.com/company/shopeefood), accessed 2026-11-19; [bento.travel food delivery](https://www.bento.travel/thailand/food-delivery), accessed 2026-11-19
- ShopeeFood merchant commission is estimated 30–35% by a third-party vendor blog; not an official rate. UNVERIFIED — [Klikit: ShopeeFood integration Thailand](https://klikit.io/en/learn/shopeefood-integration-thailand), accessed 2026-11-19
- Shopee (e-commerce) runs a Thailand CPS affiliate programme on Involve Asia, but this is for Shopee marketplace, not ShopeeFood; I did not confirm ShopeeFood is included. REPORTED — [Involve Asia: Shopee TH CPS](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program), accessed 2026-11-19 (search-result listing only)
- Quick commerce is roughly 40% of ShopeeFood volume per Momentum Works. REPORTED — [The Phuket News](https://www.thephuketnews.com/grab-line-man-duopoly-tightens-its-grip-99167.php), accessed 2026-11-19 (search-result summary)
- Robinhood history: SCB announced closure for 31 July 2024, then postponed; in Sept/Oct 2024 SCB X sold Purple Ventures (Robinhood's parent) to Yip In Tsoi–led investors for 2 billion baht (400M upfront, up to 1.6B performance-linked); the buyer scaled back and returned the business to SCB. REPORTED — [Kaohoon International, 2026-11-11](https://www.kaohooninternational.com/?p=594398), accessed 2026-11-19; [The Thaiger: closure on July 31](https://thethaiger.com/news/business/robinhood-app-to-cease-operations-on-july-31-scb-announces), accessed 2026-11-19 (search-result summary)
- Robinhood current status (as reported 11 Nov 2026): SKY ICT and Thoresen Thai Agencies each plan 45% of a vehicle (SCB X 10%) to buy Robinhood's software and brand IP; board approval reportedly obtained, signing expected by end-Nov 2026, takeover of operations December 2026, service to continue uninterrupted. Source is "a source close to the deal"; not completed. REPORTED, unconfirmed — [Kaohoon International](https://www.kaohooninternational.com/?p=594398), accessed 2026-11-19
- Bangkok Post headline "Abrupt U-turn: SCB to keep using Robinhood app" exists but the page could not be read (fetch denied), so I did not use its contents. — [Bangkok Post](https://www.bangkokpost.com/business/general/2838208/abrupt-u-turn-scb-to-keep-using-robinhood-app), accessed 2026-11-19 (headline only)
- Robinhood has no grocery/mart focus found and no developer/affiliate surface found. NOT FOUND.
- HappyFresh stopped operating in Malaysia and Thailand effective immediately in September 2022 (Bloomberg, 23 Sep 2022) and refocused on Indonesia. Not a live Thai platform. REPORTED — [Bloomberg, 2022-09-23](https://www.bloomberg.com/news/articles/2022-09-23/happyfresh-exits-malaysia-and-thailand-after-strategic-review), accessed 2026-11-19 (search-result summary); [Brand Inside](https://brandinside.asia/happyfresh-closes-its-business-in-thailand), accessed 2026-11-19 (listed in results)

### Inferences
- Robinhood's APIs, if any, are likely to change hands in Dec 2026; not a stable integration partner right now.
- ShopeeFood is the only mid-sized delivery app besides Grab/LINE MAN that is clearly active, but no consumer-integration surface found.

### Gaps
- Official ShopeeFood developer/partner/deep-link documentation; ShopeeFood merchant commission official rate.
- Final outcome of the Robinhood deal after end-Nov 2026.

## 5. Retailer online channels: 7-Eleven (7Delivery/ALL Online), CP Freshmart, Lotus's, Big C, Tops Online, Makro/Makro PRO, Gourmet Market, Villa Market, Freshket

### Takeaway
The only Thai grocery-retailer affiliate programme I could evidence is 7-Eleven's ALL ONLINE Affiliate (2024: 1% supermarket, 5% health/beauty/home & garden; open to ordinary customers; share link or QR). Lotus's affiliate programmes found were Malaysian only. No public partner APIs or documented cart/search deep links found for any retailer in this group.

### Cited Findings
- 7-Eleven ALL ONLINE Affiliate: members choose products, share product links or QR codes (Facebook, TikTok, Instagram, X, YouTube), earn commission on orders via the link; no upfront cost; open to ordinary customers. Rates by category: 1% supermarket, 5% health, beauty, home & garden. The Standard article is dated 2024-04-09; another report says "up to 5%". REPORTED, 2024 — [THE STANDARD, 2024-04-09](https://thestandard.co/7-eleven-affiliate-marketing/), accessed 2026-11-19; [Sanook](https://www.sanook.com/money/922851/), accessed 2026-11-19 (headline: "up to 5%"); [Brand Buffet](https://www.brandbuffet.in.th/2024/04/7-eleven-affiliate-program/), accessed 2026-11-19 (listed in results). Current (Nov 2026) status and rates not re-verified.
- 7-Eleven: no public developer API or deep-link documentation found for 7Delivery/ALL ONLINE (app is published by CP ALL; store listing describes delivery or 24h store pickup). NOT FOUND — [7-Eleven TH on App Store](https://apps.apple.com/th/app/7-eleven-th/id514262377), accessed 2026-11-19
- CP Freshmart: no information found on API, deep links or affiliate. NOT FOUND (not specifically searched beyond the 7-Eleven query).
- Lotus's (Ek-Chai, Thailand): searches in English and Thai found no Thai affiliate programme. Lotus's **Malaysia** has programmes on Involve Asia (per Involve, "mainly targeted at Malaysian shoppers"; commission rates not given, cookie not stated) and impact.com (2022). These are not Thai. NOT FOUND for Thailand — [Involve Asia: Lotus's affiliate programme, 2025-09-02](https://involve.asia/blog/lotuss-affiliate-program/), accessed 2026-11-19; [impact.com press release](https://impact.com/press-releases/lotuss-embraces-marketing-innovation-powered-by-impact-com/), accessed 2026-11-19 (listed in results)
- Lotus's Smart Quick, Big C Supercenter and Tops Online are named as Thai quick-commerce competitors in a paid GII report summary. UNVERIFIED — [GII Research](https://www.giiresearch.com/report/moi2062306-thailand-quick-commerce-market-share-analysis.html), accessed 2026-11-19
- Tops Online / Central Food Retail: no affiliate or commission terms found. Tops Tongtin is a supplier (SME) platform, not an affiliate scheme. NOT FOUND — [Tops corporate](https://corporate.tops.co.th/), accessed 2026-11-19 (search results)
- Central Online (Central Retail's e-commerce site) joined Involve Asia in May 2017 (older) and a Central affiliate programme page is referenced; terms/rates not read, and it is not confirmed that it covers Tops or grocery. REPORTED, old — [Involve Asia blog: Central Online](https://blog.involve.asia/affiliate-marketing/central-online-taps-involveasia-supercharge-affiliate-marketing-efforts), accessed 2026-11-19 (snippet only)
- Big C online: no affiliate/API found. GrabMart lists Big C as a retail partner. NOT FOUND (affiliate); REPORTED (Grab partnership, undated, see Section 1).
- Makro / Makro PRO: online arm of CP Axtra, described as a B2B e-commerce platform; ranked #1 grocery e-commerce in Thailand by Euromonitor (Feb 2026 release). No affiliate or API found. REPORTED — [Bangkok Post: Makro PRO top grocery platform](https://bangkokpost.com/thailand/pr/2979876/makro-pro-crowned-thailands-top-grocery-e-commerce-platform-amid-small-business-boom-), accessed 2026-11-19 (search-result summary); [CP Axtra newsroom](https://cpaxtra.com/en/newsroom/news/499/makro-pro-ranked-no1-grocery-e-commerce-platform-in-thailand-by-euromonitor-international-a-global-leader-in-market-analysis-and-research), accessed 2026-11-19 (search-result summary)
- Gourmet Market, Villa Market: no online-ordering, API, or affiliate information found (only that Gourmet Market is a GrabMart partner, undated). NOT FOUND.
- Freshket: Thai B2B food-supply marketplace for commercial kitchens; Betagro reportedly took a majority stake (e27 headline dated 2026-10-20). Third-party integrations exist (payments, Papaya POS help page lists a Freshket ordering integration); no public API docs or affiliate found. Not a consumer grocery target. REPORTED — [e27: Betagro majority stake in Freshket](https://e27.co/betagro-takes-majority-stake-in-freshket-as-thai-corporates-tighten-grip-on-startups-20261020/), accessed 2026-11-19 (headline); [Papaya help: Freshket](https://help.papaya.co.th/en/articles/11062985-freshket), accessed 2026-11-19 (listed in results)
- Involve Asia carries Thai campaigns for Shopee TH, Lazada TH, AirAsia, True TH, KTC, Nok Air (1.75% commission, 30-day cookie on the Nok Air listing). None found among these for a Thai grocer except Central Online (2017). REPORTED — [Involve Asia directory](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program), accessed 2026-11-19; [Involve Asia: Lazada Thailand guide](https://involve.asia/?p=63573), accessed 2026-11-19 (listed in results)

### Inferences
- 7-Eleven/ALL ONLINE is the clearest candidate for a link-based revenue route in Thailand: per-product links with up to 5% commission. Its fit for a grocery-list app is partial (2024 rates: only 1% on supermarket items) and unverified for Nov 2026.
- A list app would need product-level deep links to each retailer to earn anything; I found none documented. Affiliate links to product pages (as ALL ONLINE members do) are the only evidenced mechanism, which suits Phase 2 (named products) more than Phase 1 (free text).

### Gaps
- ALL ONLINE affiliate terms in Nov 2026 (current rates, whether app/automated link generation and API are permitted).
- Any Thai Lotus's/Big C/Tops affiliate via AccessTrade Thailand or direct (AccessTrade and Optimise directories not directly searched).
- URL patterns for search or cart on lotuss.co.th, bigc.co.th, tops.co.th, makro.pro, 7eleven app; not tested.
- CP Freshmart, Gourmet Market and Villa Market online status.

## 6. Is there a Thai equivalent of a "send this list, get a filled cart" API (Instacart Developer Platform)?

### Takeaway
No Thai equivalent found. The Instacart Developer Platform (US/Canada) is the reference model; in Thailand, nothing comparable surfaced for Grab, LINE MAN, ShopeeFood, 7-Eleven or the big grocers. "Not found" does not prove absence of private partner arrangements.

### Cited Findings
- Instacart Developer Platform: REST API to "add Instacart shopping capabilities to your website or app", including "Create shopping list page" and "Create a recipe page" endpoints; API key obtained via an Instacart representative; page lists en-US, en-CA, fr-CA locales (geography implied US/Canada, not stated). CONFIRMED — [Instacart Developer Platform API overview](https://docs.instacart.com/developer_platform_api/api/overview), accessed 2026-11-19
- Shopping-list endpoint takes items (name minimum) and returns a shareable link with 1–365 day expiry (default 30); the nearby-retailers lookup supports US and Canada postal codes; Instacart also runs affiliate/CPA payouts (up to $10 per new-customer order, via impact.com) for creators. REPORTED (search-result summaries and an indexed Instacart affiliate page) — [Instacart developer docs](https://docs.instacart.com/developer_platform_api); [Instacart affiliate programme](https://company.instacart.ca/affiliate), accessed 2026-11-19
- No Thai analogue found in searches across Grab, LINE MAN, ShopeeFood, Robinhood, 7-Eleven, Tops, Lotus's, Makro. See Sections 1–5.

### Inferences
- A Thai "send list, get cart" service would have to be built bilaterally, or approximated with product-page affiliate links (7-Eleven) or in-app handoff.

### Gaps
- Private partner APIs not visible publicly (e.g. Grab Mini Apps or LINE MAN partner integrations).

## 7. Merchant-side opportunities: referral fee or revenue share, precedent in Thailand

### Takeaway
Thai precedent exists for retailer-platform co-marketing and loyalty tie-ins (Tops/The 1 with GrabMart; Watsons with LINE MAN MART; bank card promotions) and for affiliate-style link commissions (7-Eleven ALL ONLINE), but I found no case of a small third-party app receiving a referral fee or revenue share from Grab or LINE MAN.

### Cited Findings
- Tops/Central Retail partnered with The 1 and GrabMart (Sept 2025) so customers earn The 1 points on GrabMart purchases through 15 Dec 2025; Watsons partnered with LINE MAN MART. These are retailer-platform-loyalty deals, not app referral deals. REPORTED — [Tops corporate news](https://corporate.tops.co.th/news/tops-under-central-retail-partners-with-the-1-and-grabmart-to-expand-into-digital-quick-commerce-market-integrating-a-loyalty-program-to-earn-the-1-points-on-grabmart-transforming-the-shopp/), accessed 2026-11-19; [Thailand Edition](https://thailandedition.com/i/line-man-mart-watsons-partner-expand-beauty-health-00a16992dab0b21546faded1), accessed 2026-11-19 (both search-result summaries)
- LINE MAN Wongnai sells advertising, coupon codes and partnership opportunities to brands via partnership@lmwn.com; Grab sells GrabAds and GrabGifts. This is paid-in, not revenue-out, for a third party. CONFIRMED — [LMWN Be Our Partner](https://lmwn.com/be-our-partner-en/); [Grab TH enterprise](https://www.grab.com/th/campaign/enterprise/), both accessed 2026-11-19
- Grab Thailand offers co-pay and merchant promotion schemes under the government's "Thais Help Thais Plus" subsidy; unrelated to app referrals. REPORTED — [Grab TH press: Thais Help Thais Plus](https://www.grab.com/th/en/press/others/grabfoodthaishelpsthaisplus/), accessed 2026-11-19 (search-result summary)
- Thai affiliate networks that run grocery/retail-adjacent campaigns: Involve Asia (Shopee, Lazada, Central Online 2017, Adidas TH, LG TH). Per-network commission terms are set by advertiser. REPORTED — [Involve Asia directory](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program), accessed 2026-11-19

### Inferences
- The most plausible Thai monetisation precedent is 7-Eleven's open affiliate (link commission). A Grab/LINE MAN referral or revenue share would be a negotiated BD deal, with no published rates or precedent for tiny apps.
- Commissions Grab/LINE MAN charge merchants (ShopeeFood ~30–35% est., GrabFood merchant promos at 9% under a scheme) show where the margin sits, but none is shared with app partners publicly.

### Gaps
- Any real-world case of a Thai app or publisher receiving a payout from GrabMart/LINE MAN Mart; none found.
- Rates and eligibility for AccessTrade Thailand and Optimise grocery campaigns, not checked directly.

## Summary table

| Platform | Operating Nov 2026? | Public API for third-party app | Deep links documented | Affiliate/referral pays app |
|---|---|---|---|---|
| Grab (GrabMart/Food/Express) | Yes | Merchant/POS only evidenced (GrabFood partner API); portal unreadable | Not found | Not found |
| LINE MAN Mart | Yes | Not found (partnership form only) | Not found | Not found |
| foodpanda/pandamart | No, closed 2025-05-23 | n/a | n/a | n/a |
| ShopeeFood | Yes | Not found | Not found | Not found |
| Robinhood | Yes; ownership change pending Dec 2026 | Not found | Not found | Not found |
| HappyFresh | No, left TH Sept 2022 | n/a | n/a | n/a |
| 7-Eleven / ALL ONLINE | Yes | Not found | Not found | Yes (2024: 1% supermarket, 5% health/beauty/home) |
| Lotus's, Big C, Tops, Makro PRO | Yes | Not found | Not found | Not found for Thailand (Lotus's Malaysia only) |
| Gourmet Market, Villa Market, CP Freshmart | Not checked | Not found | Not found | Not found |
| Freshket | Yes (B2B) | Not found | Not found | Not found |

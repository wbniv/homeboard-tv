# International list-commerce and home-screen monetization models (reference designs for a Thai household dashboard)

Conventions. All sources accessed 2026-11-19 (research date; current as of November 2026). Tags: [official] = vendor docs or press release; [press] = journalism or trade press; [vendor-marketing] = company pitching its own product; [3rd-party] = blogs, aggregators, SDK wrappers; [anecdotal] = forum or user reviews. Where a figure reached me only through a search-result summary and I did not open the page, it is marked "(snippet-level, unverified)".

## 1. Instacart Developer Platform (IDP): what it does, matching, accounts, earnings, eligibility, countries, Thai/SEA analogue

### Takeaway
IDP is a server-to-server API that turns a list of item names (or UPCs) into a hosted Instacart "shopping list page" or "recipe page" link. The user picks a store and checks out on Instacart. Developer-program partners earn a 5% commission on the whole cart within a 7-day window, paid through Impact. The program is documented only for the US and Canada. I found no Thai or Southeast Asian equivalent.

### Cited Findings
- The IDP has two suites, Shopping APIs (product discovery, cart creation) and Retailer APIs (store and location lookup). It is aimed at app developers; retailers wanting Instacart fulfilment are sent to "Connect" APIs. [official] — [IDP intro](https://docs.instacart.com/developer_platform_api/)
- `POST /idp/v1/products/products_link` "creates a shopping list page on Instacart Marketplace and generates a shareable link. When users click the link, they can select a store, add products to their cart, and check out." [official] — [Create shopping list page](https://docs.instacart.com/developer_platform_api/api/products/create_shopping_list_page/)
- Required fields are `title` and `line_items`; each line item needs at least a `name`. `link_type` is `shopping_list` (default) or `recipe`. `expires_in` has a 365-day maximum; recipe links default to 30 days, shopping-list links have no default. `landing_page_configuration.partner_linkback_url` points back to the partner's own page. Recipe pages can also set `instructions` and a 500x500 `image_url`. [official] — [same page](https://docs.instacart.com/developer_platform_api/api/products/create_shopping_list_page/)
- Matching: "Instacart uses the product name as a search term to find a matching product." If a line item carries `upcs`, "Instacart prioritizes UPC-based search"; if no UPC match exists the user is told and can search for alternatives. `product_ids` is mutually exclusive with `upcs`. `brand_filters` (case-sensitive, must match catalog spelling) and `health_filters` (ORGANIC, GLUTEN_FREE, FAT_FREE, VEGAN, KOSHER, SUGAR_FREE, LOW_FAT) are ignored when UPCs or product IDs are supplied. Quantity and unit go in `line_item_measurements`; the top-level `quantity`/`unit` fields are deprecated. Duplicate UPCs or product IDs across items return HTTP 400. [official] — [same page](https://docs.instacart.com/developer_platform_api/api/products/create_shopping_list_page/)
- Access: a development key is needed, and production needs a separate key and host (`connect.instacart.com`; dev host `connect.dev.instacart.tools`). The page does not state whether the end user needs an Instacart account to open the link. [official] — [same page](https://docs.instacart.com/developer_platform_api/api/products/create_shopping_list_page/)
- The response example carries affiliate query parameters (`aff_id`, `offer_id`, `affiliate_platform=idp_partner`), so attribution is embedded in the returned URL. [official] — [same page](https://docs.instacart.com/developer_platform_api/api/products/create_shopping_list_page/)
- Instacart's affiliate page lists three programs. Affiliate: "up to a $10 CPA ... for every new customer order". Influencer: "up to 15% on qualifying purchases". Developer (IDP): the FAQ states "5% commission of the total value of the cart from all orders completed ... within a 7 day window", though the same page also says "Affiliate terms vary depending on your user base and engagement". [official] — [Instacart affiliate programs](https://company.instacart.ca/affiliate)
- The commission applies to the whole cart, including items not on the original list; alcohol, restaurants, prescriptions and gift cards are excluded. Tracking and payment run through Impact; payout is "approximately 55 days after the end of the month"; minimum withdrawal $10. Joining is free; applications are typically reviewed in 1-2 business days. [official] — [Instacart affiliate programs](https://company.instacart.ca/affiliate)
- Eligibility: the Developer track is described for meal-planning, nutrition, cooking, health/wellness/fitness, entertainment and travel apps, plus publishers and bloggers. Affiliate eligibility requires "US and/or Canada-based traffic". [official] — [Instacart affiliate programs](https://company.instacart.ca/affiliate)
- Launch coverage (27 March 2024): IDP was described as a "publicly available" API program, but "Instacart vets all third-party partners". Launch partners were NYT Cooking and GE Appliances' SmartHQ app (oven touchscreen ordering planned later in 2024), with Weight Watchers upcoming. The article gives no commission rate. [press] — [Grocery Dive](https://grocerydive.com/news/instacart-new-api-program-recipes-ge-appliances-nyt-cooking/711418)
- The changelog shows the platform still active and updated in 2026 (an entry dated 18 June 2026 on the pre-launch checklist). [official] — [IDP changelog](https://docs.instacart.com/developer_platform_api/api/changelog)
- Thai/SEA analogue: Involve Asia lists Shopee Thailand cost-per-sale and Foodpanda Malaysia affiliate programs (rates not visible in the snippets). Lazada is an official YouTube Shopping affiliate partner in Thailand. [3rd-party] — [Shopee TH CPS (Involve Asia)](https://app.involve.asia/directory/shopee-thailand-cps-affiliate-program), [Foodpanda MY affiliate](https://app.involve.asia/directory/foodpanda-malaysia-affiliate-program)
- Grab's Thai merchant commission is cited at 9% under a co-payment promotion and 20-30% standard per order. This is the merchant-side take rate, not an affiliate payout. [press / 3rd-party] — [Grab Thailand press](https://www.grab.com/th/en/press/others/grabfoodxkonlakruengplus2025/), [Klikit Thai fee guide](https://klikit.io/en/learn/food-delivery-commission-fees-thailand-guide)

### Inferences
- A Homeboard "Order via X" button would need a server-side step to mint the link (the API key must not ship in an APK) and a store-visible product catalogue. The docs show nothing like this for Thailand.
- The 5% cart-wide rate means the incentive is basket size, not list-item conversion; "staples" lists produce small, frequent baskets.
- Because IDP is US/Canada only, it serves only as a design template: list in, hosted checkout page out, attribution carried in the URL.

### Gaps
- Instacart's official country list for IDP: not found. The affiliate page names only the US and Canada.
- Whether the IDP end user must hold an Instacart account: not stated in the docs I read. Checkout on Instacart presumably needs one.
- The 5% figure comes from a single FAQ line next to "terms vary". It could not be cross-checked against the Impact campaign page, which I did not open.
- No Thai or SEA grocery-delivery API program (Makro, Tops Online, LINE MAN, Grab Mart, Lotus's) was found. Searched once; absence of results is not proof of absence.

## 2. Retailer cart APIs and add-to-cart links (Kroger, Amazon, Walmart, Tesco/Ocado/Picnic)

### Takeaway
Retailer-side cart handoff exists in three shapes: authenticated OAuth cart-write (Kroger), a client-side GET form or URL carrying an affiliate tag (Amazon, Walmart), and nothing official (Tesco/Ocado/Picnic). Grocery commissions on the open-web affiliate programs are about 1%.

### Cited Findings
- Kroger: third-party documentation lists `PUT /cart/add`, requires the OAuth2 Authorization Code grant (customer sign-in), scope `cart.basic:write`, and UPCs, so a product-search call must come first. The API cannot read or view the cart. A rate-limit figure of 10,000 calls/day appears on an aggregator and is unverified. I could not reach developer.kroger.com (DNS failure) to confirm any of this. [3rd-party, snippet-level] — [kroger-cart (PyPI)](https://pypi.org/project/kroger-cart/), [Jentic Kroger listing](https://jentic.com/apis/kroger.com/kroger.md)
- Amazon Add-to-Cart form: multiple items are added by numbering parameters (`ASIN.1`, `Quantity.1`, `ASIN.2` ...) in a simple GET form that carries `AssociateTag` for attribution. [official, snippet-level; page fetched, parameter detail from the search summary] — [Add to Cart form doc](https://webservices.amazon.com/paapi5/documentation/add-to-cart-form.html)
- That documentation URL now returns a 302 to a notice that "PA-API 5 has been deprecated and is being replaced by the Creators API", described as REST-based and "the supported successor". No shutdown date is stated on that page. Whether the cart form survives is not stated there. [official] — [Amazon Creators API deprecation notice](https://affiliate-program.amazon.com/creatorsapi/docs/en-us/paapiv5-deprecation)
- Amazon commission: several 2026 guides put Grocery and Amazon Fresh at 1%. Attribution is a 24-hour cart window then 90 days to purchase. One blog claims Amazon cut rates by up to 50% between March and May 2026 and reduced ASIN-level reporting from 9 March 2026; this is single-source. [3rd-party, snippet-level] — [Lasso 2026 rate guide](https://getlasso.co/amazon-affiliate-commission-rate/), [Levanta policy FAQ](https://knowledge.levanta.io/articles/2364861905-amazon-associates-policy-update-faq). Amazon Associates is not a Thai-market program as far as this research shows; availability by marketplace was not verified.
- Walmart: the affiliate program runs on Impact (terms last updated April 2026). Sources disagree on grocery, at 1% to 3%, with 1% most common. No documented add-to-cart URL format was found; the tooling mentioned is a JavaScript "Buy Now" SDK via Impact. [3rd-party, snippet-level] — [Walmart affiliate terms](https://affiliates.walmart.com/terms), [Netinfluencer guide](https://www.netinfluencer.com/how-to-join-the-walmart-affiliate-program/)
- Tesco, Ocado, Picnic: no official public third-party cart or list API found. Ocado and Tesco integrations exist only as reverse-engineered or scraping tools. Whisk lists supported retailers in its own docs. [3rd-party] — [API Tracker (Picnic)](https://www.apitracker.io/a/picnic-app/developers), [uk-grocery-cli (reverse-engineered)](https://awesome.ecosyste.ms/projects/github.com%2Fabracadabra50%2Fuk-grocery-cli), [Whisk supported retailers](https://docs.whisk.com/resources/supported-retailers)
- Bring! plus EDEKA's Bringmeister (April 2019): Bringmeister suggests products inside Bring! in Berlin and Munich and forwards the user's cart to the Bringmeister shop with registration at checkout. The article calls it "another revenue source" for Bring. [press] — [iphone-ticker.de, 16 Apr 2019](https://www.iphone-ticker.de/einkaufsliste-bring-die-industrie-zahlt-mehr-als-der-nutzer-140176/)

### Inferences
- Open-web grocery affiliate rates (about 1%) are roughly an order of magnitude below Instacart's 5% IDP rate. A retailer-negotiated, list-aware deal (Bring!/Bringmeister style) beats generic affiliate links.
- Retailer cart APIs that need customer OAuth (Kroger) push friction onto the user. A link or form handoff needs no sign-in until checkout.

### Gaps
- Official Kroger Cart API terms, partner approval and Kroger's commission (if any): unreachable.
- Official Amazon grocery rate table and Creators API shutdown date: not captured; figures are blog-level.
- Any Thai retailer add-to-cart link scheme (Lotus's, Makro, Tops, Big C): not researched beyond the single search.
- Tesco, Ocado and Picnic current official partner programs (e.g. Ocado Smart Platform partner terms): not found.

## 3. Shopping-list apps: how each makes money, revenue figures, backlash

### Takeaway
Three models recur: subscription to remove ads or unlock sharing (AnyList, OurGroceries, Listonic, Bring! Premium), brand-funded in-list advertising (Bring!, the clear leader), and retailer cart handoff. Public revenue figures are essentially absent; Bring! is the only one with first-party statements about ad-led growth.

### Cited Findings
- Bring!: the company says it offers FMCG and retail partners "a mobile advertising platform", with formats including sponsored products on the list, local retailer offers, and recipe recommendations. [vendor-marketing] — [Bring about us](https://www.getbring.com/en/about-us)
- Bring! v4.0 introduced "Sponsored Product Flavors", styled to match the app, where a branded product can be found in the catalogue or search and added to the list; Bonne Maman was an early customer. [vendor-marketing] — [Bring! announcement](https://www.getbring.com/magazin/update-more-personalisation-and-new-advertising-format)
- Bring! Labs AG said it significantly expanded its advertising business in 2022, which was a key driver of revenue growth; no absolute figure. Ownership: Swiss Post acquired Bring! in 2021. [vendor-marketing, snippet-level; the page was redirected to the Bring Labs site when re-fetched] — [Bring Labs year review](https://www.getbring.com/magazin/successful-business-year-for-bring-labs-ag)
- Bring!'s current B2B pages are brand case studies (Kenwood, Kellogg's, Alpro, Spar private label, Xenia pear) and a "commerce media" pitch: "The Power of the List: How Commerce Media Bridges the Gap Between Planning and Purchase". [vendor-marketing] — [Bring Labs insights](https://www.bringlabs.com/en/insights)
- Bring! Premium: about $1.99/month or $8.99/year (US App Store), main benefit ad-free. [3rd-party, snippet-level] — [NerdWallet](https://www.nerdwallet.com/article/finance/best-grocery-list-apps), [App Store listing](https://apps.apple.com/us/app/-/id580669177)
- Bring! 2019 coverage: the app started as paid (2012), then went free and lived on advertising from retailer offer catalogues. Reader comments split: some praised "no intrusive pop-ups" and no subscription; others said the ads made the app cluttered and left for rivals, and one called data collection as payment hypocritical. [press + anecdotal reader comments] — [iphone-ticker.de, 2019](https://www.iphone-ticker.de/einkaufsliste-bring-die-industrie-zahlt-mehr-als-der-nutzer-140176/)
- AnyList: freemium subscription ("AnyList Complete"), listed around $9.99/yr individual and $14.99/yr household; a developer reply says subscription revenue covers servers and development. No ads mentioned. [3rd-party, snippet-level] — [AnyList App Store](https://apps.apple.com/ca/app/id522167641?l=en-ca)
- OurGroceries: free tier with ads on a few screens; "Remove Ads" at about $1/month (comparison-site claim). Listonic: ads, with a subscription or one-off payment to remove them (Listonic's own wording). [3rd-party / vendor-marketing, snippet-level] — [Listonic best-apps page](https://listonic.com/best-grocery-list-apps), [OurGroceries alternatives](https://startupog.com/ourgroceries-alternatives)
- Alexa shopping lists: Echo Shows carried ads "mainly through the Shopping Lists feature" before the newer full-screen ads. Amazon is also testing sponsored suggestions in Alexa shopping texts and Sponsored Products in Alexa+ conversational shopping on Echo Show (the only device where available). [press] — [TechSpot](https://www.techspot.com/news/109829-echo-show-users-amazon-smart-displays-starting-feel.html), [ContentGrip](https://www.contentgrip.com/amazon-alexa-text-shopping-ads/), [Amazon Ads](https://advertising.amazon.com/library/news/agentic-shopping-advertising)
- Todoist, Microsoft To Do, Out of Milk, Google shopping lists: not researched.

### Inferences
- The only list app showing a visible ad-led growth story (Bring!) pairs a free list with a paid ad-free tier, so the list is an ad surface first and a utility second. That is the model the owner wants to avoid in its ad form.
- Brand-sponsored "flavours" blur the line between a user's own item and an ad. Plans for Homeboard would need a clear boundary (see section 6).

### Gaps
- Absolute revenue, ad share, subscriber counts for Bring!, AnyList, Listonic, OurGroceries: none public (the one $5,000/month figure on a data site is not credible and is not used).
- Verified user backlash specific to Bring! sponsored suggestions (Reddit/HN): not found; the 2019 reader comments are the only evidence.
- Todoist, Microsoft To Do, Out of Milk, Google shopping list monetization: not covered.

## 4. Shoppable-recipe ad-tech: who pays (Chicory, Whisk/Samsung Food, Instacart)

### Takeaway
In Chicory the CPG brand pays for in-recipe ads; retailers pay for placement in the retailer picker; publishers earn a share. In Instacart's model the retailer-side marketplace pays the partner affiliate commission on the basket. Whisk (Samsung Food) is the recipe-to-retailer connector.

### Cited Findings
- Chicory (NYC, founded 2013) revenue is advertising plus software: in-line ads that promote products whenever they appear as ingredients, "Pairings" ads, and software licensed to brands, agencies and retailers. Retailers pay to be in a network of 70+ and can buy a "Featured Retailer" top slot. [vendor-marketing] — [Chicory for publishers](https://chicory.co/publishers), [Chicory for retailers](https://chicory.co/retailers)
- Chicory states a reach of 5,200 sites and 123 million monthly shoppers (self-reported). Publishers such as Warner Bros. Discovery (Food Network, Food.com) add recipe inventory. [vendor-marketing] — [Chicory-WBD announcement](https://chicory.co/blog-feed/warner-bros-discovery-recipe-sites-join-chicory-contextual-commerce-platform)
- Funding: September 2024 minority growth investment from SEVA I LP, the first outside institutional capital; amount undisclosed. Earlier totals conflict across data sites ($2.5M vs $3.6M). [press release / 3rd-party] — [Business Wire, 10 Sep 2024](https://www.businesswire.com/news/home/20240910566351/en)
- Whisk (acquired by Samsung NEXT in 2019, per one trade source) launched with 29 online grocers including Walmart, Instacart, Amazon Fresh, Peapod, and added Kroger. Samsung Food (CES 2024) is described as powered by Whisk's technology. [press] — [Grocery Dive, Whisk multi-platform app](https://www.grocerydive.com/news/walmart-instacart-partner-whisk-launches-multi-platform-app/569192/), [The Spoon, CES 2021](https://thespoon.tech/ces-2021-samsungs-smartthings-app-adding-shoppable-recipe-and-guided-cooking)
- Instacart's own recipe-page integrations (NYT Cooking, GE Appliances) pay the publisher or appliance maker a cart commission; the brand is not the payer in that flow. See section 1.

### Inferences
- Two different payers: the CPG brand (Chicory, paying for visibility) and the retailer or marketplace (Instacart-style, paying a cart commission). A Thai list widget could take either shape, but the brand-pays shape is an advertisement, which the owner has ruled out.

### Gaps
- Chicory revenue, take rate and CPMs; Samsung Food+ pricing and any shoppable revenue: not found.
- Whether Whisk's retailer integrations earn Samsung a commission, and at what rate: not found.

## 5. Smart displays and TV home screens: how platforms monetize; Telly; open-source and privacy-first launchers

### Takeaway
Roku, Google TV, and Fire TV monetize the home screen by selling banner, row and tile placements to advertisers; Echo Show now does the same, with visible user backlash. Telly pushes the model to the extreme: a free TV paid for by a permanent second ad screen. Privacy-first launchers rely on a one-time premium upgrade, with no ad revenue.

### Cited Findings
- Roku: the home screen was a grid of apps with one static banner; the CEO said video ads would be added to the banner. A later redesign reached 100 million streaming households and added ad slots for video ads, brand showcases, and sponsored playlists. [press] — [AFTVnews](https://www.aftvnews.com/roku-to-follow-fire-tv-and-google-tv-by-also-displaying-video-ads-and-more-on-its-home-screen/), [PPC Land](https://ppc.land/rokus-biggest-home-screen-change-in-a-decade-goes-live-for-100m-households/)
- Roku finances: Q4 2025 platform revenue $1.224 billion (+18%), full-year platform revenue $4.145 billion (+18%), more than 90 million streaming households at end of 2025. A secondary source gives FY2025 advertising revenue of $2.33 billion and subscriptions $1.82 billion; these sum to about $4.15 billion, consistent with the platform total. [press, snippet-level] — [CSI Magazine](https://csimagazine.com/csi/roku-2025-results.php), [PPC Land](https://ppc.land/roku-reaches-profitability-milestone-with-1-22b-platform-revenue-in-q4/)
- Google TV / Android TV: a non-removable "Sponsored" row was tested in April 2019 (Google said the point was promoting content, not quick money); in December 2024 a larger banner ad format was added under "Continue Watching". [press] — [9to5Google, Apr 2019](https://9to5google.com/2019/04/05/android-tv-sponsored-homescreen-row/), [9to5Google, Dec 2024](https://9to5google.com/2024/12/13/google-tv-homescreen-ads-new-format/)
- Fire TV: Amazon began a beta of sponsored tiles on Amazon DSP in November 2020, priced per click and open to app developers and film distributors. [press, snippet-level via a secondary source] — [PPC Land, home screen advertising](https://ppc.land/home-screen-advertising/)
- Echo Show: users report ads becoming "billboards" on the ambient home screen, and some return the device; Amazon's own pages describe Sponsored Products in Alexa+ shopping on Echo Show and Prime Video sponsored tiles. [press + anecdotal] — [TechSpot](https://www.techspot.com/news/109829-echo-show-users-amazon-smart-displays-starting-feel.html), [Tom's Guide](https://www.tomsguide.com/audio/smart-speakers/obnoxious-full-screen-ads-are-making-the-amazon-echo-show-useless-heres-how-to-fix-it)
- Telly: free 55-inch TV in the US plus a second, always-on ad "Smart Display" with a soundbar between the screens; ads are actionable (buy from the TV); sensors record viewing, ad interaction and viewer count. Telly says the data is anonymized. Founded May 2021 by Pluto TV co-founder Ilya Pozin; waitlist May 2023 (over 100,000 registrations in 36 hours per Variety); first shipments July 2023. [press via Wikipedia summary] — [Wikipedia: Telly](https://en.wikipedia.org/wiki/Telly_(company))
- Telly scale: 35,000 sets in homes at end of Q3 2025, against a 2023 goal of 500,000, per a leaked investor update reported by Lowpass; the same update allegedly said 10% of FedEx shipments arrived broken. Telly did not comment. [press, single leaked source] — [Engadget, 22 Jan 2026](https://www.engadget.com/home/home-theater/telly-has-only-delivered-35000-of-its-free-televisions-with-always-on-ads-215033698.html)
- Telly economics claim: it is reportedly making more ad money per user than Roku and similar platforms. [press, snippet-level] — [TechRadar](https://www.techradar.com/televisions/remember-the-vaguely-nightmarish-free-4k-tv-that-serves-you-ads-on-a-second-screen-its-off-to-a-rough-start-but-apparently-actually-does-make-money)
- Telly user reaction: complaints about constant ads, loud ads, quality and delivery, and tracking that cannot be disabled; Trustpilot is mixed. [anecdotal] — [Trustpilot](https://ca.trustpilot.com/review/www.telly.com), [Techdirt](https://www.techdirt.com/?p=531560)
- Projectivy Launcher (Android TV, closest analogue): ad-free, with a one-time premium upgrade (extra profiles, wallpapers, icon customization, parental controls); the listing says "Get Premium to support development". No donation option found. Its licence is disputed (SourceForge "open source", AlternativeTo "proprietary"). [press / 3rd-party] — [Android Authority](https://www.androidauthority.com/projectivy-launcher-android-tv-3684205/), [AlternativeTo](https://alternativeto.net/software/projectivy-launcher/about)

### Inferences
- Platform ad sales depend on owning a very large install base (Roku 90M+ households). A household dashboard with thousands of homes cannot sell home-screen ad inventory at scale, and the anti-ads positioning of CleanYourTV makes it a bad fit.
- Telly shows the failure mode of "free hardware, ad screen": under-delivery against plan and a privacy backlash, with revenue per user as the only upside claim.
- The Projectivy model (free core, one-time premium for convenience) is the nearest evidence for an ad-free launcher's monetization.

### Gaps
- App-placement fee schedules for any platform: not found.
- Google Nest Hub ad policy: nothing found.
- Other open-source TV launchers' funding (FLauncher, Wolf Launcher, etc.) and CleanYourTV's own funding: not found.
- Projectivy's revenue or premium uptake: not found.

## 6. Privacy-preserving commerce handoff patterns

### Takeaway
The docs show three data-flow levels: client-side form or link with items shown only on tap (Amazon form), server-minted link where item names go to the partner at link creation (Instacart), and per-user OAuth with UPC lookups (Kroger). None sends the list before the user chooses to order unless the app builds the link in advance.

### Cited Findings
- Instacart: the partner's server posts the list (names, optional UPCs, quantities, filters, a title) and gets back a URL. The list lives on Instacart's side for up to 365 days (`expires_in`) and the user remains anonymous to Instacart until they open the link. `partner_linkback_url` sends them back to the partner. [official] — [Create shopping list page](https://docs.instacart.com/developer_platform_api/api/products/create_shopping_list_page/)
- Amazon: the Add-to-Cart form is a GET form with ASIN, Quantity and AssociateTag, so nothing leaves the device until the form is submitted. [official, snippet-level] — [Add to Cart form doc](https://webservices.amazon.com/paapi5/documentation/add-to-cart-form.html)
- Kroger: adding to a cart needs the customer's OAuth authorization (Authorization Code grant), so the customer identity is known to Kroger at the point of use. [3rd-party] — [kroger-cart (PyPI)](https://pypi.org/project/kroger-cart/)
- Bring!/Bringmeister: the user's cart was forwarded to the shop with registration offered at checkout. [press] — [iphone-ticker.de, 2019](https://www.iphone-ticker.de/einkaufsliste-bring-die-industrie-zahlt-mehr-als-der-nutzer-140176/)
- Telly shows the privacy cost at the other end: viewing and ad-interaction data collection is the product. [press] — [Wikipedia: Telly](https://en.wikipedia.org/wiki/Telly_(company))

### Inferences
- The strongest design is "link minted on tap". The app builds the Instacart-style link or retailer form only when the user presses "Order", sending item names (not household identity, stock history or purchase frequency).
- An affiliate tag in the outgoing URL is the only identity-adjacent datum the partner receives, and it identifies the app, not the household.
- Unit and package size from Phase 2 can improve matching (UPCs or size filters) but add to the data sent, so make them an opt-in part of the handoff.

### Gaps
- No source found that formally audits any of these flows for privacy; the above comes from API shape and docs only.
- Thai PDPA implications of an affiliate-tagged handoff: not researched.

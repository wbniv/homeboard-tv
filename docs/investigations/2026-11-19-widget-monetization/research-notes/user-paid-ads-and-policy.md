# User-paid advertising models and the policy/legal constraints on monetizing a Thai Android TV home-screen app

Research notes for Homeboard TV (Android TV launcher, Bangkok households, Chromecast with Google TV, sideloaded APK, Google Play under evaluation). Current as of 2026-11-19. All sources accessed 2026-11-19 unless stated. This is research, not legal advice.

Evidence labels used below:
- **[fetched]** = I read the page text myself.
- **[snippet]** = the claim comes from a search-result summary only. The page itself was blocked (HTTP 403) or not opened. Verify it before relying on it.
- **[inference]** = my reasoning, not a sourced fact.

Several Google support pages (Play Payments, Play Deceptive Behavior, AdMob) render with JavaScript and returned empty bodies to the indexer. Where I quote them, the text came from a fetch tool's extraction. Re-read the live pages before quoting externally.

---

## A1. Brave Ads / BAT: revenue share, payouts, status, countries, availability to third-party apps

### Takeaway
Brave's own launch post says users get 70% of gross ad revenue, paid in BAT through Brave Rewards. 2026 trade press still describes a 70% share, but I found no 2026 Brave primary document that re-confirms it. Brave Ads appears to exist only inside the Brave browser. Payout needs a custodial or self-custody wallet, and Thailand is not confirmed as a supported payout region. I found no SDK that lets a third-party app serve Brave Ads.

### Cited Findings
- Launch statement: "Users receive 70% of the gross ad revenue, while Brave receives the remaining 30%. Users are rewarded with Basic Attention Tokens (BAT) via the integrated Brave Rewards in their browser." This is a 2019 launch post, so it is old. — [Brave: Brave Launches the First Advertising Platform Built on Privacy](https://brave.com/brave-ads-launch/) [fetched]
- Ads are opt-in. Users must enable Brave Rewards. Ad matching happens on the device, and Brave says "a user's data is never sent to anyone, including Brave." — [Brave launch post](https://brave.com/brave-ads-launch/) [fetched]
- Launch geographies were "the U.S., Canada, France, Germany, and the U.K. More geographies will be available soon." Thailand was not among them. — [Brave launch post](https://brave.com/brave-ads-launch/) [fetched]
- Publisher-integrated ads had a different split in the same post: 70% to verified publishers, 15% to users, 15% to Brave. This is a separate product. — [Brave launch post](https://brave.com/brave-ads-launch/) [fetched]
- 2026 status: PPC Land (citing an AdExchanger report of 2026-03-24) says Brave Ads "operated with a 70 percent revenue share to users." It describes opt-in rewarded ads since 2019, plus newer new-tab takeover display ads and limited pop-up notifications. BAT can be moved out of the Rewards wallet and converted to other digital assets or fiat. — [PPC Land: Brave blocks your ads and sells its own](https://ppc.land/brave-blocks-your-ads-and-sells-its-own-and-its-working/) [fetched]
- Brave's payout-custodian list is published as "List of supported regions: Payout accounts," last updated 2025-08-18 per a search summary. Uphold is available in multiple regions, bitFlyer is Japan-only, ZebPay is India-only, Gemini is US-only, and a Solana self-custody option is also listed. I could not open the page (HTTP 403). — [Brave Support: List of supported regions](https://support.brave.app/hc/en-us/articles/6539887971469-List-of-supported-regions-Payout-accounts) [snippet]
- Brave staff said in May 2024 that Gemini was not accepting new connections. A 2022 community post names Uphold, Gemini and bitFlyer as the custodial partners. The custodian country is generally set by the ID documents used with that provider. — [Brave Community: Non supported regions (2022)](https://community.brave.app/t/non-supported-regions/407820) [fetched]; [Brave Community search hits, 2024–2025](https://community.brave.com/t/gemini-and-uphold-are-not-supported-in-several-countries/564439) [snippet]
- A Brave community thread says Thailand is not supported for linking Rewards to Uphold, citing the official list. I could not verify this against the live list. — [Brave Community: Brave Rewards and connecting Uphold not working](https://community.brave.com/t/brave-rewards-and-connecting-uphold-not-working/619193) [snippet]
- Brave Ads for advertisers is a self-serve program inside the Brave Ads and Rewards ecosystem (April 2023). — [Brave: Self-Serve Ads](https://brave.com/blog/self-serve-ads/) [linked from the launch page; not opened]

### Inferences
- [inference] Brave Ads is a feature of the Brave browser, not a platform for third parties. Homeboard cannot "plug in" Brave Ads. The most Homeboard could do is point a user to Brave on the same device.
- [inference] Even for Brave users, a Thai user likely cannot withdraw BAT through Uphold, Gemini or bitFlyer. Brave Ads therefore does not give a Thai household a simple cash payout, and a Homeboard integration would inherit that problem.

### Gaps
- No official 2026 Brave page confirming the 70% share or the list of countries where ads are served. Only 2019 launch text and 2026 trade press.
- Whether Thailand is a supported Brave Ads country, and whether a Thai resident can connect any payout account. The official regions page was blocked.
- Whether Brave offers any SDK, API or partner program to third-party apps. I found none. That is absence of evidence, not proof.
- Whether Brave Rewards works inside Brave on Android TV. Not checked.

---

## A2. Telly: economics, data collected, how it went

### Takeaway
Telly gives away a 55-inch TV with a second ad screen and monetizes through ads plus viewing and presence data. It does not pay users. Its only user payment is a planned "Telly Rewards" gift-card scheme. By Q3 2025 it had 35,000 TVs in homes against an original 500,000 goal, but reportedly earned about $52 per TV per month in ad revenue. That figure is unconfirmed.

### Cited Findings
- Business model: a free 4K TV with a smaller second "Smart Screen" below the soundbar. Ads sit on that screen. Ads could extend onto the main screen when it is idle, "depending on whether the advertiser wants to get creative." — [TechRadar (2023)](https://www.techradar.com/news/this-free-tv-has-a-second-screen-for-extra-ads-and-it-could-be-a-privacy-nightmare) [fetched]
- Telly said it was "working on 'Telly Rewards' that will reward users with a gift card… for participating in [on-screen polls]." — [TechRadar (2023)](https://www.techradar.com/news/this-free-tv-has-a-second-screen-for-extra-ads-and-it-could-be-a-privacy-nightmare) [fetched]
- Data collected, per Telly's Viewing and Activity Data Policy as quoted by TechRadar: the audio and video content watched and for how long; interaction with the TV (search queries, remote button presses); and "the physical presence of you and any other individuals using the TV at any given time." TechRadar presumes this relies on the built-in camera. — [TechRadar (2023)](https://www.techradar.com/news/this-free-tv-has-a-second-screen-for-extra-ads-and-it-could-be-a-privacy-nightmare) [fetched]
- Opt-out: the data policy reserves the right to collect this data. Opting out means returning the TV or reimbursing its presumed cost. — search summary of [AdGuard](https://adguard.com/blog/free-telly-ads-privacy.html), [Tuta](https://tuta.com/blog/free-tv-telly-privacy-nightmare) and [IAPP](https://iapp.org/news/b/free-smart-tv-startups-privacy-notice-comes-with-strings-attached) [snippet]. I opened the IAPP page, but the extract returned only site navigation.
- Telly missed its 500,000-unit goal for 2023. — [NextTV via Yahoo](https://www.yahoo.com/entertainment/telly-falls-short-500k-customer-225354333.html) [snippet]
- Late 2025: a leaked November 2025 investor update, reported by Lowpass and then Ars Technica (2026-01-23), put Telly at 35,000 TVs in homes at the end of Q3 2025 (28,000 the quarter before). It reported about $22M annualized revenue, roughly $52 per TV per month by Ars's arithmetic. Roughly 10% of FedEx-shipped units arrived broken. Telly declined to comment on the specifics. — [Lowpass](https://www.lowpass.cc/p/telly-35000-tvs-10-percent-breakage) [snippet]; [Engadget](https://www.engadget.com/home/home-theater/telly-has-only-delivered-35000-of-its-free-televisions-with-always-on-ads-215033698.html) [snippet]; [TechRadar](https://www.techradar.com/televisions/remember-the-vaguely-nightmarish-free-4k-tv-that-serves-you-ads-on-a-second-screen-its-off-to-a-rough-start-but-apparently-actually-does-make-money) [snippet]
- Comparison used by the press: Roku's average revenue per user was $41.49 for 2024. — Ars/Lowpass via the search summary above [snippet]
- Telly's ad partners include Nielsen (measurement), Microsoft, MNTN and Magnite. — search summary above [snippet]. Telly says its collected data is anonymized and used only to improve the ad experience. — [Wikipedia: Telly (company)](https://en.wikipedia.org/wiki/Telly_(company)) (surfaced in the index; secondary)

### Inferences
- [inference] Telly is not a user-paid-ads model. The user is paid with hardware, not cash or points. The $50-ish per month per device is a US figure for an always-on, premium ad slot with hardware subsidy and camera-derived presence data. It is not a benchmark for a widget on a Thai home screen.
- [inference] The lesson for Homeboard is that the data-for-subsidy trade drew heavy privacy criticism. Homeboard's positioning as an anti-ad, anti-bloat fork conflicts with Telly-style always-on ads and presence data.

### Gaps
- Telly's current (2026) corporate status. I found only the Q3 2025 investor update, which is unconfirmed by Telly.
- Whether "Telly Rewards" ever launched.
- Direct text of Telly's opt-out terms. I have only secondary summaries.

---

## A3. Other opt-in user-paid models (Gener8, Google Opinion Rewards, Swagbucks/Mistplay, Samsung/Microsoft Rewards, data dividend)

### Takeaway
Only Google Opinion Rewards is verified as available in Thailand. It pays Google Play credit for surveys and is a Google app, not an SDK. I could not verify Gener8, Samsung Rewards, Microsoft Rewards, Swagbucks or Mistplay for Thailand, and I did not research the data-dividend proposals. No cash-paying rewarded-attention SDK for third-party Android apps was found.

### Cited Findings
- Google Opinion Rewards launched in Thailand in November 2020. On Android, payout is Google Play credit, which expires if unused for a year. Surveys pay between $0.10 and $2. Users must reach a $2 balance first. — [Wikipedia: Google Opinion Rewards](https://en.wikipedia.org/wiki/Google_Opinion_Rewards) [fetched; only the first 100,000 of 128,000 characters read]
- Google's own country list includes Thailand. Eligibility is adults 18 and over. — [Google Opinion Rewards: App availability and eligibility](https://support.google.com/opinionrewards/answer/6322274) [snippet; page body did not render]
- Swagbucks: the only evidence is one Malaysian user's forum post saying the site was active only in the US and Canada. There is no official country list. — [Mozilla Support thread](https://support.mozilla.org/ms/questions/1225736) [snippet; weak]
- A related Brave-based model: Brave Rewards redemption has been run through a partner network, TAP Network. — [Brave launch post](https://brave.com/brave-ads-launch/) [fetched]

### Inferences
- [inference] Google Opinion Rewards confirms that a "paid to opt in" attention product exists in Thailand, but it pays in Play credit (a closed loop), not cash.
- [inference] A third-party app cannot pay "cash" through Google's ad stack (see A4). Any cash or points scheme would have to be run by Homeboard, with the legal burdens in B6.

### Gaps
- Gener8: one search returned nothing on its current status. Not established.
- Samsung Rewards, Microsoft Rewards, Mistplay, Swagbucks: Thailand availability not found.
- "Data dividend" proposals (for example California, Andrew Yang): not researched.
- No cash or points-paying rewarded-attention SDK for third-party apps was found in the sources I checked. Not exhaustive.

---

## A4. Rewarded ad SDKs (AdMob, AppLovin, Unity): can the reward be cash?

### Takeaway
No. AdMob's policy bars direct monetary items "under any circumstance." Indirect rewards are allowed only if redeemable inside the publisher's own app, non-transferable, and capped at 25% for physical-item discounts. I did not retrieve AppLovin's or Unity's policies.

### Cited Findings
- Policy text (extracted from the live AdMob policy page): "Direct monetary items may not be offered as rewards under any circumstance." Defined as "any form of legal tender, or other modes of payment that can be used directly to purchase goods or services in the real world." — [AdMob: Policies for ads that offer rewards](https://support.google.com/admob/answer/7313578?hl=en) [fetched via extraction tool; the HTML body was blank to the indexer]
- Indirect items are allowed when "the reward is only redeemable and usable for an item or service within the publisher's platform, website or app"; physical items or loyalty points "that can be transferred to another person are not permitted"; and "rewards that are a discount or voucher for physical items must not exceed 25% of the item's total value." — same page [fetched via extraction tool]
- Trade press from late 2023 describes this as a loosening that allows indirect monetary items such as in-app credits, while cash, cryptocurrency and gift cards stay prohibited. — [Search Engine Land: AdMob policy update](https://searchengineland.com/admob-policy-update-indirect-monetary-items-can-now-be-offered-as-rewards-433157) [snippet]
- Rewarded ads must be opt-in, with clear disclosure of the required action and the reward before each ad. — AdMob policy via search summary [snippet]
- Google Play Ads policy: the full-screen interstitial rules "do not apply to rewarded ads which are explicitly opted-in by users (for example, an ad that developers explicitly offer a user to watch in exchange for unlocking a specific game feature or a piece of content)." — [Google Play Ads policy](https://play.google.com/about/monetization-ads/ads/ad-id) [fetched]

### Inferences
- [inference] Gift cards are explicitly listed as direct monetary items by AdMob, so "watch an AdMob rewarded ad, get a grocery voucher" is prohibited. A closed-loop credit inside Homeboard is the most the AdMob rules allow. Whether other SDKs differ is unverified.
- [inference] A direct-sold, non-SDK model (Homeboard sells its own sponsor slots and shares revenue itself) sidesteps the AdMob reward rule. It still faces the Play and legal constraints below.

### Gaps
- AppLovin MAX and Unity LevelPlay/Ads reward policies: not retrieved. Treat as unknown. They are mostly game-oriented networks.
- Whether AdMob serves ads on Android TV or CTV at all in a way a home-screen app could use. Not checked.

---

## A5. Revenue figures: CPMs for CTV and rewarded ads in Thailand/SEA, and what a user could earn

### Takeaway
I found no sourced Thailand-specific CTV or rewarded CPM. The only SEA numbers are weak vendor claims of about $1–4 for rewarded video. The best hard datum is Telly's reported US figure of about $52 per TV per month, which does not transfer to Thailand. Any per-user monthly earnings figure for a Thai household is therefore an assumption.

### Cited Findings
- TopOn's 2025 H1 report ranks Southeast Asia, South Asia and Latin America at the bottom of overall eCPM. It ranks formats Rewarded Video > Interstitial > Splash > Native > Banner and notes rewarded-video eCPM was up 18% year on year. These are global gaming figures. — [TopOn Global Mobile Games Monetization Report 2025 H1](https://mores.toponad.com/reports/TopOn%20Global%20Mobile%20Games%20Monetization%20Report%20_%202025%20H1.pdf) [snippet]
- One vendor blog says SEA eCPMs "can be much lower, often ranging from $1 to $4." Its origin is unclear. — [Applixir: Optimizing the Right Reward for Video Ads](https://www.applixir.com/?p=21) [snippet; weak]
- A 2026 vendor benchmark gives rewarded video $16–20 eCPM with no regional split. It is likely US/tier-1 weighted. — [Playio: Rewarded ad benchmarks 2026](https://blog.playio.co/rewarded-ad-benchmarks-2026) [snippet]
- Telly's reported figure: about $52 per TV per month, about $22M annualized on 35,000 TVs. See A2. Roku's 2024 average revenue per user was $41.49 (per the same reporting).
- No Thailand CTV CPM was found. A Nielsen item covers YouTube CTV measurement in Thailand with no prices. — [Nielsen Thailand](https://www.nielsen.com/news-center/2024/nielsen-launches-deduplicated-youtube-ctv-campaign-measurement-in-thailand/) [snippet]

### Inferences
- [inference, my arithmetic, assumption-driven] If one rewarded view earned a $2 eCPM, then 100 views per day would be $0.20 per day, or about $6 gross per month. A 70% user share would be about $4.20 per month. The $2 input is from the weak vendor range and the 100 views per day is a guess. A household earning single-digit US dollars per month looks like the plausible order of magnitude, not hundreds.
- [inference] Brave's 70% share would be 70% of an equally small base. The share percentage matters less than ad volume and CPM.

### Gaps
- Thailand rewarded-video eCPM from an authoritative source (for example an AdMob or AppLovin country report).
- Any CTV or Android TV ad CPM for Thailand.
- Fill rates for home-screen ad placements.

---

## B1. Google Play policy: affiliate links, Play Billing, launcher/home-screen ads, deceptive and disruptive ads, incentivized actions, Android TV quality

### Takeaway
Play's Ads policy is the strongest constraint. Ads "may only be displayed inside of the app serving them" and must not interfere with the device, and the policy names "overlays, companion functionality, and widgetized ad units." Grocery purchases made outside Play are exempt from Play Billing. Android TV quality criteria restrict clickable ads from linking to a web URL or another app, which affects affiliate links shown on TV. I found no Play text specific to affiliate disclosure.

### Cited Findings
- Disruptive ads: "Your app cannot force a user to click an ad or submit personal information for advertising purposes before they can fully use an app. Ads may only be displayed inside of the app serving them and must not interfere with other apps, ads, or the operation of the device, including system or device buttons and ports. This includes overlays, companion functionality, and widgetized ad units. If your app displays ads or other ads that interfere with normal use, they must be easily dismissible without penalty." — [Google Play Ads policy](https://play.google.com/about/monetization-ads/ads/ad-id) [fetched]
- Better ads experiences: full-screen interstitials that show unexpectedly are not allowed, and they must be closeable after 15 seconds. The rule "does not apply to rewarded ads which are explicitly opted-in by users." — [Google Play Ads policy](https://play.google.com/about/monetization-ads/ads/ad-id) [fetched]
- Advertising ID: the ID "must only be used for advertising and user analytics," the opt-out status "must be verified on each access," and it may be tied to personally identifiable information only "with the explicit consent of the user." — [Google Play Ads policy](https://play.google.com/about/monetization-ads/ads/ad-id) [fetched]
- Payments policy page: Play Billing is not required for "Purchases or rentals of physical goods (such as groceries, clothing, home appliances, or electronics)" or "Purchases of physical services (such as transportation services, airfare, gym memberships, or food delivery)." It is required "for developers offering in-app purchases of digital goods and services." The page I extracted does not address affiliate or referral payments. — [Play Console Help: Payments](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en) [fetched via extraction tool]
- Android TV app quality (Tier 3, TV Ready) advertising criteria: TV-AP (interact with ads using D-pad); TV-AD (full-screen non-video ads immediately dismissible); TV-AU ("for advertising that uses clickable, non-fullscreen, non-video ads, the app does not let ads link to a web URL"); TV-AA (clickable ads must not link to another application, initiate cross-package navigation, or trigger an install, updated January 2027); TV-AN (new January 2027: "All ads must prevent navigation disruption and either remain in-context (inside the current application) or use off-screen conversion mechanisms (such as QR codes)"). — [Android Developers: TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality) [fetched]
- Same page: the manifest must declare ACTION_MAIN with CATEGORY_LEANBACK_LAUNCHER (TV-ML); the app must have a launcher icon and a 320x180 banner (TV-LM, TV-LB); and "the app does not partially obscure other apps" (TV-TR). — [Android Developers: TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality) [fetched]

### Inferences
- [inference] An ad widget inside Homeboard's own home screen is Homeboard's own surface, so it probably sits within "displayed inside of the app serving them." But the phrase "widgetized ad units" and the launcher's role as system-like UI make review risk real. A home screen where ads may block other apps or system buttons is the case Play most plausibly flags.
- [inference] TV-AU and TV-AN suggest an affiliate link in a clickable ad should not open a web URL on TV. A QR code to the phone is the sanctioned off-screen conversion path. Whether these criteria are enforced as rejection reasons or are advisory quality tiers is unclear.
- [inference] The grocery-affiliate use case is a physical-goods referral, so Play Billing would not apply. That is separate from the Ads policy review.

### Gaps
- Play "Deceptive Behavior" and "Device and Network Abuse" policy text: both pages returned blank bodies. No quote available. In particular, I found no text on lock-screen, system-UI or launcher ads beyond the Ads policy above.
- Any Play policy text on affiliate-link disclosure. None found.
- Play "incentivized actions" policy (paying users to install or rate): not retrieved.
- Whether the 2027 TV criteria changes are enforced in Play review.

---

## B2. Can a third-party launcher be the default on Google TV, and does Play accept launcher apps for TV?

### Takeaway
There is no official Google path to make a third-party launcher the default home screen on Google TV. Community methods exist, they are fragile, and recent updates have blocked some of them. Third-party launchers such as Projectivy are reported to be distributed through Play. I found no Google policy text on launcher apps for TV.

### Cited Findings
- On Google TV the stock launcher stays the real HOME and the remote's Home button is hard-wired to it, so "install launcher, set as default" silently does not work. A working method needs ADB `cmd package set-home-activity` plus an accessibility service that intercepts the Home key. It was tested on three TVs and failed on one. — [Mustafa.net: Replace the Google TV launcher without root (2026-11-01)](https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/) [fetched]
- A Google TV update reportedly stopped Projectivy from overriding the default home screen. It still opens manually. — [Android Authority: Google TV update blocks Projectivy override](https://www.androidauthority.com/google-tv-update-blocks-projectivy-override-3649445) [snippet; page 403, date not captured]
- The stock launcher (`com.google.android.apps.tv.launcherx`) reportedly registers its HOME intent at priority 2. — a project page cited in search results [snippet; low reliability]
- Android Authority's launcher-alternative walkthrough tells users to install a launcher such as Projectivy from the Play Store on Google TV. — [Android Authority: install Google TV launcher alternative](https://www.androidauthority.com/install-google-tv-launcher-alternative-3663642/) [snippet]
- TV quality criteria require CATEGORY_LEANBACK_LAUNCHER for any TV app, which is the tag for an app launcher icon, not a home replacement. — [Android Developers: TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality) [fetched]

### Inferences
- [inference] Homeboard as a "launcher" is likely in practice an ordinary leanback app plus a user-applied ADB or accessibility workaround. Its distribution does not need Google's endorsement, but a Play listing as a home replacement is unverified.
- [inference] The Mustafa.net workaround is an accessibility service. Google Play restricts accessibility-service use, so a Play build that relies on it carries separate policy risk. I did not retrieve the Play policy to confirm that.

### Gaps
- No Google documentation on third-party home replacement on Google TV.
- No Play policy text on launcher or home-screen apps for TV.
- Date and affected software versions for the Projectivy-blocking update.

---

## B3. Thailand PDPA: consent for sharing shopping-list or household data; cross-border transfer

### Takeaway
PDPA consent must be explicit, separate from other messages, freely given and withdrawable. Sharing grocery-list contents with affiliate partners or ad networks needs a lawful basis, usually consent. Sending the data abroad needs an adequacy finding, a safeguard such as standard contractual clauses, or a properly informed consent. Fines run to THB 5 million, with criminal and civil exposure as well.

### Cited Findings
- Consent: "explicitly made in writing or via electronic means" and "clearly separated from other messages"; regulator guidance calls for a clear affirmative action; consent must be "freely given and not conditional on entering into a contract"; it can be withdrawn at any time. — [DLA Piper: Data protection laws, Thailand](https://www.dlapiperdataprotection.com/index.html?t=law&c=TH) [fetched via extraction tool; secondary]
- Direct marketing: individuals may object, and controllers must keep an opt-out function "throughout the entire processing period." — [DLA Piper: Thailand](https://www.dlapiperdataprotection.com/index.html?t=law&c=TH) [fetched; secondary]
- Sensitive data (health, biometrics and other listed categories) needs explicit consent. — [DLA Piper: Thailand](https://www.dlapiperdataprotection.com/index.html?t=law&c=TH) [fetched; secondary]
- Penalties: administrative fines up to THB 5,000,000 (late breach notification up to THB 3 million); criminal liability for responsible directors or managers; punitive damages up to twice actual compensation. DLA Piper notes six fines imposed since June 2022. — [DLA Piper: Thailand](https://www.dlapiperdataprotection.com/index.html?t=law&c=TH) [fetched; secondary]
- Extraterritorial reach: the PDPA applies to organisations handling data of Thai residents "regardless of whether they are formed or recognised under Thai law." — [DLA Piper: Thailand](https://www.dlapiperdataprotection.com/index.html?t=law&c=TH) [fetched; secondary]
- A DPO is needed for large-scale monitoring or sensitive data. A "large amount" is 100,000 or more data subjects. — [DLA Piper: Thailand](https://www.dlapiperdataprotection.com/index.html?t=law&c=TH) [fetched; secondary]
- Cross-border: the PDPC issued two notifications under sections 28 and 29 on 2023-12-25, effective 2024-03-24. The destination must have "adequate data protection standards," judged by legal measures, a regulatory authority, and similar factors. Otherwise safeguards apply, including standard contractual clauses (a Thai Model and an Overseas Model are both accepted), binding corporate rules and certification. — [Tilleke & Gibbins: Thailand Unveils Regulations for Cross-Border Personal Data Transfer (2024-01-04)](https://www.tilleke.com/insights/thailand-unveils-regulations-for-cross-border-personal-data-transfer/) [fetched; secondary]
- Consent as a transfer route works only if the data subject is first told the destination lacks adequate protection. Law-firm commentary says this suits occasional transfers, not routine ones. — [FORSLAW via Austcham Thailand](https://www.austchamthailand.com/resources/news/forslaw-cross-border-customer-data-under-thailands-pdpa) [snippet; secondary]

### Inferences
- [inference] A grocery list can reveal health conditions, religion (halal), pregnancy and similar traits, so it could be treated as sensitive data in some cases. That would raise the consent bar. This is my reading, not a sourced ruling.
- [inference] Passing list contents to a US-based ad network or affiliate network is a cross-border transfer needing a section 28/29 basis. A design that sends no list contents (affiliate links that carry only an item search term chosen by the user, or on-device matching) avoids most of this.

### Gaps
- Primary text of the PDPA and of the PDPC notifications (pdpc.or.th). I used law-firm and advisory summaries only.
- Whether the PDPC has published an adequacy list of countries.
- Whether a household shopping list is "personal data" when not linked to a name. Under the PDPA, data that identifies a person "directly or indirectly" is covered, which probably includes device-linked data. I did not verify that point.

---

## B4. Thai advertising disclosure rules for affiliate or sponsored content (OCPB)

### Takeaway
The current baseline is the 1979 Consumer Protection Act. A draft amendment, open for consultation from 2026-09-09 to 2026-10-10, would create a duty for influencers and affiliate marketers to disclose that content is advertising and reveal their relationship with the business. It was not enacted at the time of the sources.

### Cited Findings
- The draft amendment defines a new "hired advertising person for selling goods or services" covering "influencers, content creators, live streamers, affiliate marketers, and virtual online media operators who receive monetary compensation or other benefits." Such persons "must disclose to consumers that content is advertising and reveal their relationship with the business owner." Disclosure applies when the business pays, employs, or provides free or discounted products or services, and the consumer would not otherwise know of the connection. — [Tilleke & Gibbins: Thai Consumer Protection Overhaul (2026-09-17)](https://www.tilleke.com/insights/thai-consumer-protection-overhaul-to-address-digital-commerce-and-influencer-marketing) [fetched; secondary]
- Consultation period: 2026-09-09 to 2026-10-10. — [Baker McKenzie: Thailand proposed amendments to Consumer Protection Act (2026-09)](https://www.bakermckenzie.com/en/insight/publications/2026/09/thailand-proposed-amendments-to-thailands-consumer-protection-act) [snippet; page 403]
- On 2026-09-10 the OCPB and five advertising-sector partners signed voluntary guidelines with the Federation of Thai SMEs covering reviews, livestreams, product links and AI-generated content. — [Pattaya Mail](https://www.pattayamail.com/thailandnews/thailand-sets-new-rules-for-influencers-and-trusted-smes-564684) [snippet; secondary]
- Hashtag labels such as #ad or #paidpartnership may help, but the sources do not establish any specific label as legally required. — search summary of law-firm commentary [snippet]
- ETDA issued guidelines (2024) for ads on digital platform services: verify advertiser identity (minimum IAL2), review ads before publication, keep records. They apply to DPS operators, not to ordinary apps. — [Tilleke & Gibbins: Thailand Issues Guidelines for Advertising on Digital Platforms (2024-10-08)](https://www.tilleke.com/insights/thailand-issues-guidelines-for-advertising-on-digital-platforms/) [fetched; secondary]

### Inferences
- [inference] If the draft passes as described, a grocery-list widget that earns commission on links would likely fall in the "affiliate marketer" definition. A visible "we earn a commission" label near each link is the safe design regardless of enactment.
- [inference] This is a plan-for-it item, not a current blocker, as of the sources.

### Gaps
- The OCPB's own text of the voluntary guidelines and the draft. I used law-firm summaries only.
- Enactment status after the 2026-10-10 consultation close.
- Whether the rules apply to an app (a service) rather than a content creator. The definition names people and "virtual online media operators."

---

## B5. Royal Decree on Digital Platform Services (ETDA): thresholds and whether a small app that facilitates purchases must notify

### Takeaway
The decree covers electronic intermediary services that connect consumers and merchants "regardless of whether a service fee is charged." Platforms offering only the operator's own goods or an affiliate's are excluded. Notification applies to in-scope operators, with a later deadline for small or low-impact platforms. I could not retrieve the exact user and revenue thresholds from a primary source.

### Cited Findings
- Scope: "electronic intermediary services that create a connection between consumers, merchants or businesses, or other types of users in order to create an electronic transaction in whole or in part, regardless of whether a service fee is charged." — [Tilleke & Gibbins: Deadline Looms for Digital Platform Notification Requirement (2023-11-07)](https://www.tilleke.com/insights/deadline-looms-for-digital-platform-notification-requirement-in-thailand) [fetched; secondary]
- Excluded: services that offer only the goods or services of a single operator or an affiliated agent. — same page [fetched]
- Deadlines: 2023-11-18, or 2024-08-20 "for small or low-impact platforms." Non-compliance carries criminal fines and imprisonment (amounts not given). — same page [fetched]
- The decree was published in the Government Gazette on 2022-12-22 with a 240-day grace period. Providers supervised by other authorities, such as the Bank of Thailand or the SEC, are exempt from notifying ETDA. Overseas operators in scope must appoint a coordinating person in Thailand. — [Tilleke & Gibbins: Thailand Issues Royal Decree on Digital Platforms](https://www.tilleke.com/insights/thailand-issues-royal-decree-on-digital-platforms/) [fetched; secondary]
- Reported triggers for extra information: more than 5,000 monthly users in Thailand. Earlier-draft revenue tests were THB 1.8M a year for individuals and THB 50M for corporate entities (unconfirmed against the final text). The large-platform tier is revenue above THB 300M per service type, or THB 1,000M across all types, or users above 10% of Thailand's population. — Tilleke commentary via search summary [snippet; **not verified in fetched text**]
- ETDA said in June 2025 it would step up enforcement. — [Tilleke & Gibbins: Thailand to step up enforcement](https://www.tilleke.com/insights/thailand-to-step-up-enforcement-of-royal-decree-on-digital-platform-services/) [snippet]

### Inferences
- [inference] A widget that links a shopping list to outside merchants may be an "intermediary" connecting consumers and merchants, so it could fall in scope. The single-operator exclusion would not apply. Whether a plain outbound affiliate link "creates an electronic transaction in part" is unclear, and I did not find an authority on it.
- [inference] If Homeboard were in scope, it would probably be a "small or low-impact" platform, with a basic notification duty. Verify with counsel or ETDA.

### Gaps
- The decree's and ETDA's primary text, with the exact thresholds (etda.or.th).
- Whether outbound affiliate links count as intermediary services.
- Current (2026) amendments to the thresholds. The search found none.

---

## B6. Paying users in Thailand: promotions, lotteries, e-money/stored value, tax withholding

### Takeaway
A fixed, predictable payment for watching ads looks different from a prize draw. Random prizes ("lucky draw") need a permit under the 1935 Gambling Act, and cash cannot replace the licensed prize. Holding or issuing stored value for users may touch the Payment Systems Act. Prizes and promotional rewards are subject to withholding tax. None of this is settled for a deterministic revenue share, which I found no source on.

### Cited Findings
- Lucky draws: the Gambling Act B.E. 2478 (1935) treats a lucky draw as a "lot drawing" activity needing a licence. Without one, the party awarding the prize faces criminal penalties. Processing takes at least 15 working days. Cash or cheques cannot substitute for the licensed prize. Prizes above THB 1,000 are subject to 5% withholding tax. — [Tilleke & Gibbins: Legal requirements for lucky draws](https://www.tilleke.com/insights/legal-requirements-lucky-draws) [snippet; older article, date not captured]
- Reform: a public hearing on a new draft Gambling Act ran in June–July 2026 (per a 2026 Tilleke note), and a Sweepstakes Bill would move some promotions to notification instead of permission. Status unclear. — [Mondaq: Thailand's gambling law reform](https://www.mondaq.com/article/1858824/thailands-gambling-law-reform-developments-to-watch) [snippet]
- E-money: under the Payment Systems Act B.E. 2560, e-money is a licensed "designated payment service." KPMG lists minimum capital of THB 100 million for e-money issuers of pre-paid cards or e-wallets. I could not confirm any exemption for closed-loop loyalty points. — [KPMG Thailand Legal News Update, issue 23 (2023-03)](https://kpmg.com/th/en/home/insights/2023/03/legal-news-update-issue-23.html) [snippet]
- Withholding tax: the government's guide puts prizes from competitions and raffles at 5% (Section 40(8) income). A separate page puts rewards or discounts resulting from sales promotion at 3%. A temporary 2% rate ran 2020-10-01 to 2022-12-31 for e-withholding only. An older regulation says prize tax is at the income-tax rate, which conflicts with the flat 5%. — [Thailand.go.th: withholding tax rates for personal income recipients](https://thailand.go.th/event-detail/006_134) [snippet]; [KPMG Thailand tax news flash 107 (2021-09)](https://kpmg.com/th/en/home/insights/2021/09/th-tax-news-flash-issue-107.html) [snippet]

### Inferences
- [inference] A scheme of "view ad, earn a fixed amount" is not a lottery, and it avoids the Gambling Act if no chance element exists. Sweepstake or "random bonus" mechanics would trigger it.
- [inference] Paying cash through a third party (a PromptPay transfer, a payout processor) avoids Homeboard holding balances, which would be the e-money question. Homeboard issuing its own wallet balance is the risky path.
- [inference] Payments to individuals for attention may be taxable income, and a business paying them may have withholding duties. The rate and form for this case were not found.

### Gaps
- The full, dated text of the Tilleke lucky-draw article. I have snippet-level information only.
- Whether a Thai-law e-money licence is triggered by a small closed-loop points balance. Not confirmed.
- Withholding treatment of a fixed ad-attention payment to individuals. Not found.
- Primary sources: bot.or.th and rd.go.th pages were not reached.

---

## B7. Securities referral: would linking market-data widgets to brokers need an SEC Thailand licence?

### Takeaway
Not answered. Advising the public on securities for a fee or other remuneration is a licensed activity, but I found no authority on whether a referral or affiliate link to a broker counts. That needs a Thai securities lawyer or a written SEC query.

### Cited Findings
- Under the Securities and Exchange Act, "investment advisory service" means giving advice to the public, directly or indirectly, about the value of securities or the suitability of investing in them, "for a fee or other remuneration." A licence from the Finance Minister on the SEC's recommendation is required. — [Tilleke & Gibbins: Regulation of investment advisors in Thailand](https://www.tilleke.com/insights/regulation-investment-advisors-thailand/) [snippet; secondary]
- The SEC's September 2024 guidelines for foreign operators list factors that count as doing business in Thailand: a Thai-language website or application, accepting Thai baht, and facilitating Thai investors to use offshore services directly. — [Baker McKenzie: SEC guidelines for foreign business operators](https://insightplus.bakermckenzie.com/bm/capital-markets/thailand-the-secs-first-official-guidelines-for-foreign-business-operators-providing-investment-services-to-thai-investors) [snippet]; [SEC statement 2024-09-18](https://sec.or.th/EN/Template3/SECStatement/2024/SECstatementEN-180924.pdf) [snippet]
- Providers already supervised by the SEC are outside the ETDA notification regime. — [Tilleke & Gibbins: Royal Decree on Digital Platforms](https://www.tilleke.com/insights/thailand-issues-royal-decree-on-digital-platforms/) [fetched]

### Inferences
- [inference] Showing neutral market data with no advice is unlikely to be "advisory service." A broker link that carries recommendation language ("best broker to buy X") moves toward advice for remuneration. The SEC's own approach to affiliates and "introducing" arrangements is unknown to me.

### Gaps
- No source on whether referral commissions or affiliate links to brokers need an SEC licence. Not found.
- SEC primary pages on advertising or solicitation by non-licensed parties.
- Any rules on market-data redistribution (exchange licensing for data feeds). Not researched.

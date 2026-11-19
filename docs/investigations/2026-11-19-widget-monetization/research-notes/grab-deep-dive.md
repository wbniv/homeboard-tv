# Grab follow-up: what the developer portal actually offers

Will, 2026‑11‑19: "what about grab? i order most of my groceries through grab". The first pass
([thai-delivery-platforms.md](thai-delivery-platforms.md)) could not read Grab's developer portal,
because it is rendered by JavaScript and a plain fetch returned an empty page. This follow-up
rendered the portal in a real browser (Chrome) on 2026‑11‑19. Labels: **CONFIRMED** means read on
the official page; **UNVERIFIED** means search results only.

## Developer portal catalogue (CONFIRMED)

The [docs index](https://developer.grab.com/docs/) lists these APIs:

| Area | API |
|---|---|
| Delivery | Express API |
| Mall | GrabMall API |
| Payments | One-time Charge, GrabPay QR (two versions), Integrations Launchpad for PayLater and GrabPay, Grab POS API v3, Tokenisation |
| GrabKios | Digital Products, Biller Gateway |
| Partner Apps | Integrate your Partner App |
| Food | GrabFood API |
| Identity | Login With Grab |
| Rides | Farefeed |
| Grab For Business | Partner API, GrabGifts integration guide |
| Mart | GrabMart API |
| GrabDefence | Risk Evaluation |
| Loyalty | Points Earning API, GrabRewards Tier |
| Communications | GrabChat Partner API, GrabChat Web UI |

None of these is an affiliate or referral programme, and none adds items to a customer's GrabMart cart.

## GrabMart API: merchant point-of-sale only (CONFIRMED)

The [GrabMart Partner API (POS) Integration Guide v1.1.3](https://developer.grab.com/docs/grabmart/api/v1-1-3/)
covers authorisation, self-serve activation, menu sync, order sync (submit order webhook,
accept/reject, edit, mark ready, delivery state, cancel), store hours and status, and
merchant campaigns. It is for shops that sell on GrabMart. It does not serve consumer apps.

## GrabExpress API: shippers can book deliveries in Thailand (CONFIRMED)

The [GrabExpress API reference](https://developer.grab.com/docs/grab-express/) is
"for external partners using GrabExpress":

- **Endpoints:** quotes, create, details, cancel, a tracking webhook and tips.
- **Authentication:** OAuth2 client credentials with scope `grab_express.partner_deliveries`.
- **Coverage:** Thailand (THB) is listed, and Bangkok-region multi-city delivery is allowed for INSTANT.
- **Access:** production is enabled per country and city by a GrabExpress business contact.

This is how a local shop could deliver a household's list. Grab does not pay the app that sends the order.

## Partner Apps: put a web app inside the Grab super app (CONFIRMED)

[Partner Apps](https://developer.grab.com/docs/partner-apps/) "bring your web application
directly into Grab's super app". The integration gives a native webview, Grab authentication,
payments, push notifications, GrabCoins, and promotion through contextual placements and GrabAds.

- **Current partners:** Firsty (eSIM), HelloRide, Drive lah, redBus and Jolibox.
- **Onboarding:** through partnerapp.partnerships@grabtaxi.com.

This is a sales channel for services that charge users. It is not a payout to a list app.

## GrabRewards Points Earning API (CONFIRMED, from the official PDF)

The [Points Earning API](https://developer.grab.com/assets/docs/grab-rewards/Rewards_Events_API.pdf)
"enables your business to reward your customers" with GrabRewards points.

1. The partner looks up a Grab user by phone number or email and enrols them.
2. It stores the returned `partnerUserID`.
3. It posts events (`POST /rewards/v3/events`) with a source, a country code and parameters.

The GrabRewards Operation team configures a points formula for each source and country, for
example `points = transactionAmount * 0.001`. The PDF does not say who pays for the points; I
infer the partner does. This is a route to paying Grab users in a currency they already
value. It would cost Homeboard money and needs a partnership.

## Grab already does list-to-cart itself (CONFIRMED)

Will, 2026‑11‑19: "is someone already doing shopping list integration with grab?"

- **Grab Shopping Agent.** [Grab, 2026‑04‑08](https://www.grab.com/inside-grab/stories/grabx-ai-shopping-agent-grocery-list/):
  "users can simply upload their shopping list and let the agent do the heavy lifting". It is
  "now available in Indonesia, Malaysia, Philippines, Singapore, Thailand and Vietnam". The flow:
    1. Choose a merchant and tap the GrabMart shopping-list icon.
    2. Create or edit a list "by typing, speaking, or uploading a photo".
    3. Review the suggested matches, add them to the cart and order.
- **Grab AI Assistant (GrabX 2026, Jakarta, 2026‑04‑08).** It builds "checkout-ready shopping
  carts from a photo, voice note, or typed list" ([The Paypers](https://thepaypers.com/payments/news/grab-launches-13-ai-powered-features-across-consumer-travel-and-merchant-services)).
  Sources conflict on how far it has rolled out beyond Singapore.
- **Meta Muse pilot (Singapore).** Grab "teamed with Meta to pilot the company's Muse artificial
  intelligence assistant in Singapore, letting customers build GrabMart grocery baskets directly
  within chat interfaces before completing checkout inside the Grab app"
  ([PYMNTS, 2026‑11‑03](https://www.pymnts.com/earnings/2026/grab-gets-ai-assistance-as-revenues-rise-27percent/)).
  It is a private partnership; no public API is documented.
- **Third-party list apps:** searches found none that hand lists to Grab. That is not proof none exist.

## Not found

- **Deep links:** no documented `grab://` scheme or `screenType` parameter. Searches found nothing
  official, so a search, store or cart prefill into GrabMart is UNVERIFIED. Grab uses its own short
  links (`grb.to`) in campaigns. Settle it by reading the intent filters of the Grab app on an
  Android phone (`adb shell dumpsys package com.grabtaxi.passenger`).
- **Affiliate or referral payouts:** none on the portal, on Grab's Thai pages or on the affiliate
  networks searched.

## Itemised receipts (UNVERIFIED for layout)

GrabMart supports an e‑Tax invoice. At checkout the customer opts in, enters taxpayer details and
an email address, and receives a full tax invoice. Grab's Thai terms say it lists the goods bought
from the store, excluding delivery and other fees. Sources: the
[GrabMart Easy E-receipt page](https://www.grab.com/th/en/blog/grabmart-easy-e-receipt/) and the
[Grab press release](https://www.grab.com/th/en/press/others/easyereceipt/). The latest campaign
found ran 2025‑01‑16 to 2025‑02‑28. Whether the invoice is available outside tax-scheme campaigns,
and what the email looks like, needs one real order to confirm.

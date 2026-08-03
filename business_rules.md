# 5.1 Business Rules

This section defines the business rules for the Hotel Booking Assistant Application. These rules impose constraints, logic, and validations on user operations, database transactions, check-in/check-out procedures, payment integrations, loyalty systems, and AI modules.

---

### Module 1: Authentication & Account Management

BR-01
Each account must use a unique email address and phone number within the system. The database shall enforce unique constraints on these fields, and any registration or update request containing duplicates shall be rejected with an appropriate error message.

BR-02
Customers must successfully log in before accessing secure functions such as room booking, payment, or eKYC verification. Unauthenticated requests to restricted API endpoints shall be blocked by the system security filter and returned with an HTTP 401 Unauthorized status.

BR-03
User passwords must be encrypted before being stored in the database. The system shall utilize the BCrypt hashing algorithm to encrypt passwords during registration and password reset, and raw passwords must never be stored in plain text or logged.

BR-04
Accounts with an Inactive or Banned status are not permitted to access the system. Upon login, the authentication service must verify the account status, and if it is not set to Active, the login request shall be denied with a message informing the user of the account restriction.

BR-05
The system shall generate a 6-digit OTP code with a validity period of 5 minutes during registration. This code must be sent to the user's email address and stored in Redis with an automatic expiration time (TTL) matching the validity period.

BR-06
Registration is completed only when the user enters the correct OTP matching their registered email within the 5-minute limit. If the OTP expires or is incorrect, the temporary registration details in Redis shall be cleaned up, requiring the user to restart the registration process.

BR-07
The system shall temporarily restrict login attempts for 1 minute for an email address after 5 consecutive failed login attempts. This rate limit must be enforced using Redis key trackers to prevent brute-force attacks on user credentials.

BR-08
Successful authentication must issue a JSON Web Token (JWT) containing the user's role and identity claims. The token must be signed using a secure algorithm and must be included in the Authorization header of subsequent secure API requests to grant access.

BR-09
Forgotten password reset requests must require a verified OTP sent to the registered email before generating a secure reset token. The reset token must be a randomly generated UUID linked to the user's email in Redis.

BR-10
Password reset tokens stored in Redis shall expire after 3600 seconds. The reset token must be automatically deleted from the cache upon expiration or successful password reset to prevent unauthorized reuse.

BR-11
Changing the password while logged in requires verification of the current password against the stored password hash. The request shall be rejected if the provided current password does not match the decrypted hash in the database.

BR-12
The system shall support Google Sign-in to automatically authenticate and map users to customer profiles. If a matching Google email does not exist in the database, the system shall automatically create a new customer account with default credentials.

BR-13
Registered users must not modify their email address, ID card number, or role when updating their profile. These fields are strictly read-only after account registration to maintain data integrity and prevent identity fraud.

---

### Module 2: Customer Management

BR-14
New customer profiles registered via the public signup flow are assigned a default status of "Active". The default role assigned to these accounts must be set to "customer" within the database user entity.

BR-15
All customer accounts must contain mandatory non-null fields including full name, phone number, email, and ID card number. The ID card number must consist of exactly 12 digits, matching standard Vietnamese CCCD/CMND formatting.

BR-16
Staff and manager administrative modifications of customer statuses must be recorded in the audit logs. The audit log must record the administrator ID, the target customer ID, the old and new status values, and the exact timestamp.

---

### Module 3: Room Type Management

BR-17
Room types must define name, base price, adult capacity, child capacity, total capacity, and status. The status field must restrict values to active or inactive to indicate if the room type is available for online booking.

BR-18
The base price of a room type must be a positive value greater than or equal to zero. The database table shall enforce a positive constraint on this decimal field to prevent errors during price calculations.

BR-19
Total capacity of a room type must be dynamically computed as the sum of its adult and child capacities. This derived field must be automatically recalculated whenever the adult or child capacity parameters are updated.

BR-20
A room type cannot be deleted if there are active rooms or future bookings associated with it. The database shall enforce referential integrity constraints to prevent deletion, returning an error message if active references exist.

---

### Module 4: Room Management

BR-21
Every room must have a unique room number within the hotel database. The database must reject insertion of rooms with identical room numbers to prevent room assignment duplication.

BR-22
Rooms must only transition between states: Available, Reserved, Occupied, Cleaning, and Maintenance. The system must enforce these state transition rules through backend service checks.

BR-23
Upon checkout, the room status must automatically transition to Cleaning. This transition must be triggered by the check-out service completion and will block the room from check-in until cleaning is completed.

BR-24
Rooms in Cleaning or Maintenance status must not be assigned to new check-ins or bookings. The room selection algorithm for reservations and check-in must filter out rooms in these statuses.

BR-25
Every room must be configured with an admin passcode for master override and service access. The passcode must be stored in encrypted format and accessible only by authorized staff roles.

---

### Module 5: Booking Management

BR-26
The booking check-in date must be earlier than the check-out date, and both must be set in the future. The system must validate that the check-in date is at least one day prior to the check-out date during booking creation.

BR-27
Every booking must generate a unique, non-duplicative reference code of up to 20 characters. The booking reference must be generated using an alphanumeric sequence that is easily readable by users.

BR-28
Online bookings default to a Pending status and require a verified deposit or payment to be Confirmed. If no payment is detected within the designated transaction window, the booking shall transition to Cancelled automatically.

BR-29
Available room search calculations must exclude rooms with conflicting bookings or statuses during the requested stay window. The search query must check overlapping date ranges to prevent double-booking.

BR-30
The total number of guests in a booking must not exceed the combined capacity of the selected room types. The system must validate that the number of adults and children falls within the specified capacity limits.

BR-31
Walk-in bookings created by staff at the counter must allow direct room assignment and transition directly to Checked-in. The reservation system must bypass the standard online deposit checks for counter bookings.

BR-32
Group bookings must allow reserving multiple rooms across different room types under a single booking reference. The billing system must aggregate room costs, taxes, and service fees under a master invoice.

BR-33
The booking creation transaction must place a lock on room availability to prevent double-booking conflicts. This database lock must remain active during payment processing to secure the reservation.

BR-34
Customers can submit stay extension requests only for bookings currently in the Checked-in status. The request must create a pending StayExtension customer request that requires manual approval and payment.

BR-35
Room changes are only allowed if the target room is in the Available status. The room change service must verify the status of the destination room before executing the change.

BR-36
Room upgrades require payment of the price difference, while room downgrades process refunds based on cancellation policy rules. The financial recalculation must be performed dynamically by the room change service.

BR-37
Booking modifications or cancellations must be requested before the check-in date and are subject to active cancellation policies. The system must restrict online cancellation requests after the check-in date has passed.

---

### Module 6: Check-in Management

BR-38
Self-service check-in via QR Code or Face Recognition is only permitted for customers with a verified eKYC profile. The check-in validation service must verify the eKYC status of the customer before granting access tokens.

BR-39
The customer eKYC profile must contain valid uploaded images of the ID card front, ID card back, and a face selfie. The OCR service must parse the document details and verify they match the user profile fields.

BR-40
Face registration for eKYC requires uploading 5 images at different angles: front, left, right, up, and down. The face registration service must extract and store embeddings for each angle to support subsequent validation.

BR-41
Face Recognition check-in endpoints must restrict execution authority to users with the Manager role. The system security context must validate that the actor email belongs to a manager before executing verification.

BR-42
Face check-in liveness verification must validate camera frames using active challenge directions (left or right). The AI service must compare the sequence of challenge frames to detect static picture spoofing.

BR-43
The system shall permit verified eKYC customers to generate a short-term QR Code token for check-in. The QR Code must contain a encrypted payload representing the booking details.

BR-44
Generated QR Code tokens for check-in must expire after a maximum duration of 10 minutes. Upon expiration, the customer must request a new token through the application interface.

BR-45
Check-in is only allowed on the scheduled check-in date after the standard check-in hour unless early check-in is approved by staff. The check-in validation service must enforce the standard check-in hour limit.

BR-46
Successful check-in must generate a digital room key access code and update the room key status to Active. The digital key must be stored in the database and displayed to the customer via the mobile app.

---

### Module 7: Check-out Management

BR-47
A booking must be in Checked-in or Staying status before a check-out can be processed. The system must restrict check-out requests for bookings that are cancelled or completed.

BR-48
All consumed services and additional charges must be fully billed to the booking invoice before checkout. The check-out service must aggregate all unpaid bookingservices linked to the booking ID.

BR-49
Check-outs occurring after the standard checkout hour (e.g. 12:00 PM) must incur late check-out fees. The late checkout calculation must apply incremental hourly fees as configured in the system.

BR-50
A check-out cannot be finalized until the booking final amount has been paid in full. The system must verify that the paid amount equals the final amount before completing checkout.

BR-51
Finalizing a check-out must immediately transition the room status to Cleaning. The check-out transaction must automatically set the room status in the database.

---

### Module 8: Payment Management

BR-52
PayPal transactions must capture order payments, verify amounts, and record sandbox transaction codes. The payment logging service must verify the transaction payload from PayPal webhook before confirming the booking.

BR-53
The system must support online payments via domestic payment gateways such as VNPay. The billing service must process callback request signatures to verify payment authenticity.

BR-54
Staff must log manual cash payments on the booking, updating the paid amount field. The system must record the cashier ID and transaction note alongside the payment record.

BR-55
Confirmed bookings require a minimum deposit payment of 50% of the total booking amount. The system must enforce this deposit minimum check during online payment processing.

BR-56
PayPal payments must programmatically convert the booking amount from VND to USD using the configured exchange rate. The exchange rate multiplier must be loaded dynamically from the database configuration.

BR-57
Cancelled bookings must trigger automatic refund estimation based on active cancellation policies and create a pending Refund customer request. The system must check cancellation date offsets to calculate refund percentages.

BR-58
Approved refunds for online PayPal bookings must be processed programmatically using the original transaction code. The refund service must invoke the PayPal API refund endpoint using sandbox credentials.

BR-59
Finalizing a check-out must generate a detailed PDF invoice listing all room charges, service charges, taxes, and payments. The PDF must be generated dynamically and emailed to the guest.

---

### Module 9: Voucher & Promotion

BR-60
Vouchers applied to a booking must be verified as active, within their expiration date, and below their usage limit. The voucher service must check coupon validity checks before modifying total booking amounts.

BR-61
Voucher discounts are only applied if the booking amount meets the minimum purchase threshold of the voucher. The discount logic must check the room total amount before applying discount rates.

BR-62
Promotion rates are only applicable if the booking dates overlap with the active promotional campaign period. The system must check start and end dates of active promotions during search.

BR-63
Customer loyalty discounts are applied to the base rate first, before applying any additional voucher discounts. The loyalty system must apply discount percentages based on user membership tier.

BR-64
Applied voucher discounts must be capped at the voucher's maximum discount value limit. The calculation engine must truncate the discount value if it exceeds the configured maximum limit.

---

### Module 10: Customer Loyalty

BR-65
Completed bookings must award customer reward points at a rate of 1 point per 10,000 VND spent. The points calculation must exclude tax and promotional discount values.

BR-66
Customers can redeem accumulated reward points to pay for bookings at a conversion rate of 1 point to 100 VND. The system must deduct redeemed points from the customer account balance upon booking confirmation.

BR-67
Membership tiers must upgrade automatically based on lifetime reward points: Silver (1,000), Gold (5,000), Platinum (10,000). The user service must evaluate point thresholds upon completion of every booking check-out.

BR-68
Membership tiers must grant rate discounts: Silver (2%), Gold (5%), Platinum (10%). The pricing calculator must apply these tier discounts to room rates during checkout.

BR-69
Personalized AI recommendations require the customer to have at least one completed booking in the system. The AI model must use historical preferences to personalize room suggestions.

---

### Module 11: AI Features

BR-70
The AI recommendation model must process guest preferences, travel intent, price limits, and history to recommend room types. The prediction payload must return similarity scores for matching hotel room types.

BR-71
The ML dynamic pricing algorithm must use the room type's base price as the minimum price floor. Dynamic rates calculated by the model must never fall below this baseline value.

BR-72
Dynamic pricing calculations must apply seasonal multiplier factors during holidays, weekends, or peak periods. The multipliers must be configured in the administrative dashboard pricing table.

BR-73
Room rates must increase dynamically as real-time occupancy of the corresponding room type exceeds 70%, 80%, and 95%. The system must recalculate the rate adjustments hourly based on booking counts.

BR-74
The system must recalculate dynamic prices daily and update the active price cache. The pricing service must flush old cache entries to ensure fresh daily rate changes.

BR-75
In the event of a Python AI pricing engine failure, the system must fall back to the default static base rate. The booking service must handle connection timeouts gracefully and log fallback events.

---

### Module 12: Dashboard & Statistics

BR-76
Revenue statistics calculations must aggregate all successful payments minus processed refund amounts. The financial dashboard must display net revenue totals for selected date ranges.

BR-77
Daily occupancy rate statistics must be calculated as: (number of occupied rooms / total active rooms) * 100%. The stats service must compute this value daily at midnight.

BR-78
Average Daily Rate (ADR) must be calculated as: total room revenue / number of sold rooms. This metric must be updated daily and displayed on the manager dashboard.

BR-79
Revenue Per Available Room (RevPAR) must be calculated as: total room revenue / total available rooms. The dashboard must plot monthly RevPAR trends for manager review.

BR-80
Returning customer statistics must count the number of customer accounts with more than one completed booking. The dashboard must display returning guest percentages to evaluate customer loyalty.

BR-81
Dashboards must display real-time check-in, check-out, and staying statistics for the current day. The stats widget must refresh counts using WebSocket notifications.

---

### Module 13: Search Functions

BR-82
Room search requests must support filtering by room type, dates, price, capacity, and amenities. The specification builder must construct dynamic SQL queries based on search parameters.

BR-83
Customer search functions for staff must support filtering by name, phone number, email, and ID card number. The query must use indexes to return search results quickly.

BR-84
Text-based search fields must utilize case-insensitive substring matching. The SQL queries must use database functions like `LOWER()` or `ILIKE` for partial text matching.

---

### Module 14: Authorization

BR-85
Customers are restricted to booking, payment, profile updates, QR check-in token generation, and history views. The authorization filter must block customers from editing rooms or viewing dashboards.

BR-86
Receptionists are authorized to perform check-in, check-out, walk-in bookings, manual payment logging, search, and key issuance. The system must verify these receptionist role privileges at runtime.

BR-87
Managers are authorized to manage room types, rooms, view revenue dashboards, approve refunds, and execute Face check-in. Manager actions must bypass standard restriction filters.

BR-88
The system must validate a JWT access token in the authorization header and enforce role-based access controls on secure endpoints. Requests without valid signatures or expired tokens shall be rejected immediately.

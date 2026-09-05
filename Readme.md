## DaysSincePro

The rebirth. What's old is new again.<br>
Or as Yogi Berra would say, "It's Deja Vu all over again. When you come to the fork in the road, take it."

Back in 2019 i stumbled upon and became enamored with the utility of Alex Mak's Days Since Pro. For 12-18mo, as i do, i peppered him with feedback on the app, and more often than not he was responsive and engaged in a dialog with me about various bug fixes i suggested and potential DCR's (design change requests) i highlighted. At some point, as life does... he drifted off from actively maintaining the app. And i continued to use it... though a particular feature implementation issue around the 'Days Until' functionality would on a recurring basis return to my foreground processing, and i'd think about reaching out to Alex again to see if he'd arrived at a solution and would consider taking time to make an update. At some point in the 1st half of 2026 i reached out to Alex again, and lo and behold he noted he'd published the source up on GitHub, and i could have at it if i wished... so i promptly implemented the fix i'd been dreaming about and a test harness to validate it, but for another 4mo or so never got around to actually setting up the build environment and the undertaking around republishing the app on Google Play Store (which at this point in time requires a 14day closed trial w/a dozen users, which takes some doing).

Finally, in late August 2026 i've gotten around to doing this— so here we are. What else can i say-- it's simply been Days Since.<br>
A sincere thanks to Alex Mak.<br>

This branch is a fork of Alex’s original repository and includes all source code along with my ongoing modifications to support publishing and maintaining the app on the Google Play Store.<br><br>

One other word of thanks; Big UP to to Mustafa and [**App Hive**](https://play.google.com/store/apps/details?id=com.codignia.apphive)
!<br>
These days, as a dev publishing an app to Google Play Store, you're gated with completion of a 14 day [**Closed Test**](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) with a min of 12 users testing your app over that time. No one bailing out, or skipping a day, etc or your time clock resets... not easy to find those folks, coordinate common start, keep everyone on track, engaged, and rewarded... irrespective of fixing ongoing feedback, etc; a massive uphill slog of a logistical nightmare at best, and in practice an arduous, near insurmountable burden for the individual, personal developer.<br>
Enter [**App Hive**](https://play.google.com/store/apps/details?id=com.codignia.apphive)
: https://play.google.com/store/apps/details?id=com.codignia.apphive <br>
[**App Hive**](https://play.google.com/store/apps/details?id=com.codignia.apphive)
handles all the details of crowdsourcing a great group of devs with a common goal and timeline, and coordinates daily testing of each by the others, day in and day out, until the group assists others in their Hive to pass the gate, in reciprocol fashion. Simply outstanding! A wonder Google hasn't acquired them yet. And what a burden it takes off you as an individual dev. I can't imagine what an uphill climb it would be without [**App Hive**](https://play.google.com/store/apps/details?id=com.codignia.apphive)
; and you won't believe just how straightforward it makes flying through the Google Console/Play Store Closed Test gate until you try it!

Regards-- Marc
<br><br><br>

What follows below remains untouched as Alex wrote the ReadMe.md on publishing it out to GitHub in 2024:

## Days Since Pro 3

The first version of _Days Since Pro_ was released on Sep 18, 2016 in the Google Play Store as a replacement
to the app's predecessor _Days Since Lists_, which was launched even earlier.

_Days Since Lists_ was designed for Android 2.0 and was created with a very early way of creating android apps
through an Eclipse plugin. _Days Since Pro_ is a big improvement over its predecessor and added features from
many user suggestions.

Users can enter any number of life events and the app simply tell you have many days since that event or how
many days until the event. The app let the user organize in categories, and present the data in several formats.

Each entry is stored on your phone in a [SQLite](https://www.sqlite.org/) data file.

The app had over 10,000 users, had many 5 star reviews and was installed all over the world.

## Google Play Store

As of Nov 2024, my Google Play app store is closed.

## Open Source

I here provide the source code for Days Since Pro.
This source code will compile with Android Studio, and you can create an APK file and install in your Android device.

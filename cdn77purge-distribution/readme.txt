How to run it:
==============

Make sure you have java in your path.

run "doit.bat" inside a cmd.exe to update all files that are different

or "doit.bat --all" to clear the whole cache. Please run "doit.bat" 10 minutes later to fill cache.

What does it do?
================

It downloads the sitemap.xml from www2 and compares all files there the ones on www.
If any differences are found, it tells the CDN to refresh the page on www.

The execution time is less than 1 minute.

Further problems
================

It seems that WP and TotalCache doesn't handle 301 redirects perfectly.

Therefore, it seems that we have to select "Clear all caches" from the Performance menu" on the top after adding new 301 redirects

Please wait 10 minutes before running again
===========================================

It takes a couple of minutes for the CDN to refresh, so run once, wait 10 minutes, run again

Example
=======

C:\data3\clojure\cdn77-purge\linux build>java -jar cdn77purge-0.1.0-SNAPSHOT-standalone.jar
Starting...
Sep 19, 2015 9:14:57 AM clojure.lang.Reflector invokeMatchingMethod
INFO: prefetched  (/support/online-help/themedesigner-overview/)
...Finished

This means that one file wasn't updated, and we try to make a refresh.
If you run again, maybe not all files disappear. These remaining files should be 301 redirect pages, where we have 
an issue with wordpress. websearchpro is looking at this.

If a file doesn't disappear and it is not a 301 page, please let me know and I will investigate.

/mattias

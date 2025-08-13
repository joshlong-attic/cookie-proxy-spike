# cookie from headers translation demo

chrome plugins cant send cookies, even if they can read them, from a chrome tab. so we need to let them send a header and then use a spring cloud gateway filter to translate them. this example 'leaks' httponly cookies so that we can then test that we can use them via a custom header in another incognito tab.

in a real, production applicatin, we'd never use `httponly=false` because that would open us up to cross-site forgery attacks. but the plugin has the ability to read thee users cookie, explicitly, by requesting permission from the user. this allows the plugins to then use that cookie to make requests on behalf of the user.

so a simple google chrome plugin flow would be:

* user clicks extension.
* it opens up a tab in chrome. not an iframe in the plugin window. not a plugin popup. a regular tab for the user,  with all the regular permissions of any tab. if the user wants to proceed, they can.
* the user can then do the OAuth dance, with all the redirects that this entails
* once this is done, the plugin will be able to read the cookies - which is the JSESSIONID connecting the browser to tghe OAuth session on the gateway.
* the plugin can't make requests with that token,however, so we'll use spring cloud gateway to translate the presence of a header into a cookie. so the plugin will read the cookie value, and then set a custom header for all requests. that header ges translated into a cookie and the server responds as though the requests were coming directly from the brower tab itself.

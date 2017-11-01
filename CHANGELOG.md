0.2.2

 * Ignore links with the `data-trigger` attribute, since they trigger other actions.

0.2.1
 * Fixes a bug around query params and hash fragments due to inproper setup of Html5History.

0.2.0
 * Fixes a bug wherein clicking a link to the same page the user was on would trigger a full page refresh.

0.1.9
 * Fixes a bug introduced in 0.1.8 whereby the port wouldn't be properly set on different browsers.

0.1.8
 * Fixes a bug wherein the history duplicates if the relative href is equal to the current active one.
 * Accountant no longer blocks default action when the target is set to something other than "" or "\_self", which means pop-ups are no longer blocked.
 * Check for differences in port number when handling the link.

0.1.7
 * Fixes a bug where accountant wouldn't reload when the target had a different
 hostname
 * Removes dependency on Secretary, allowing for use with other routing
 libraries.

0.1.6
 * Fixes a bug where query parameters would persist at different paths.
 * Adds support for URL hash fragments.

0.1.5
 * Internal refactoring
 * Extended browser event gating for meta key, alt key, ctrl key, shift key, etc.

0.1.4
 * Modify `navigate!` to not suppress browser events when the user tries to open a new tab

0.1.3
 * Adds core.async as a dependency
 * Adds a `dispatch-current!` function to dispatch on page load.

0.1.2
 * Adding navigation support via the HTML5 history API.

0.1.1
 * Initial release.

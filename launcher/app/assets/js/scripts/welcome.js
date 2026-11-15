/**
 * Script for welcome.ejs
 */
document.getElementById('welcomeButton').addEventListener('click', e => {
    loginCancelEnabled(false)
    loginViewOnSuccess = VIEWS.landing
    switchView(VIEWS.welcome, VIEWS.login)
})

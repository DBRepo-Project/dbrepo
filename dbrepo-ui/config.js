const config = {}

config.title = process.env.NODE_ENV !== 'development' ? process.env.TITLE : 'Database Repository'
config.icon = process.env.NODE_ENV !== 'development' ? process.env.ICON : '/favicon.ico'
config.brokerUsername = process.env.NODE_ENV !== 'development' ? process.env.BROKER_USERNAME : 'fda'
config.brokerPassword = process.env.NODE_ENV !== 'development' ? process.env.BROKER_PASSWORD : 'fda'
config.brokerLoginUrl = process.env.NODE_ENV !== 'development' ? process.env.BROKER_LOGIN_URL : '/admin/broker/'
config.keycloakLoginUrl = process.env.NODE_ENV !== 'development' ? process.env.KEYCLOAK_LOGIN_URL : '/api/auth/'
config.openSearchUrl = process.env.NODE_ENV !== 'development' ? process.env.OPENSEARCH_LOGIN_URL : '/admin/dashboard/'
config.version = process.env.NODE_ENV !== 'development' ? process.env.VERSION : 'vue-dev'
config.logo = process.env.NODE_ENV !== 'development' ? process.env.LOGO : '/logo.png'
config.searchUsername = process.env.NODE_ENV !== 'development' ? process.env.SEARCH_USERNAME : 'admin'
config.searchPassword = process.env.NODE_ENV !== 'development' ? process.env.SEARCH_PASSWORD : 'admin'
config.clientId = process.env.NODE_ENV !== 'development' ? process.env.DBREPO_CLIENT_ID : 'dbrepo-client'
config.clientSecret = process.env.NODE_ENV !== 'development' ? process.env.DBREPO_CLIENT_SECRET : 'MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG'
config.defaultPublisher = process.env.NODE_ENV !== 'development' ? process.env.DEFAULT_PID_PUBLISHER : ''
config.doiUrl = process.env.NODE_ENV !== 'development' ? process.env.DOI_URL : ''
config.uploadPath = process.env.NODE_ENV !== 'development' ? process.env.UPLOAD_PATH : '/tmp/'

module.exports = config

const config = {}

config.title = process.env.TITLE
config.icon = process.env.ICON
config.brokerUsername = process.env.BROKER_USERNAME
config.brokerPassword = process.env.BROKER_PASSWORD
config.brokerLoginUrl = process.env.BROKER_LOGIN_URL
config.keycloakLoginUrl = process.env.KEYCLOAK_LOGIN_URL
config.openSearchUrl = process.env.OPENSEARCH_LOGIN_URL
config.sharedFilesystem = process.env.SHARED_FILESYSTEM
config.version = process.env.VERSION
config.logo = process.env.LOGO
config.tokenMax = process.env.TOKEN_MAX
config.searchUsername = process.env.SEARCH_USERNAME
config.searchPassword = process.env.SEARCH_PASSWORD
config.clientId = process.env.DBREPO_CLIENT_ID
config.clientSecret = process.env.DBREPO_CLIENT_SECRET
config.defaultPublisher = process.env.DEFAULT_PID_PUBLISHER
config.doiUrl = process.env.DOI_URL
config.uploadPath = process.env.UPLOAD_PATH

module.exports = config

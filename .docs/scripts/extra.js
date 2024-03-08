function getVersion() {
    const segments = location.pathname.split('/');
    if (segments.length > 4 && (segments[3].match(/[0-9]\.[0-9]\.[0-9]/g) || segments[3] == 'latest')) {
        console.debug('version', segments[3]);
        return segments[3];
    }
    console.debug('default version', '__APPVERSION__');
    return '__APPVERSION__';
}
function switchVersion(value) {
    if (!value) { return }
    this.location.href = location.protocol + '//' + location.host + '/infrastructures/dbrepo/' + value + '/';
}
document.getElementById("version").value = getVersion();
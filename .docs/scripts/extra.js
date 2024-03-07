function getVersion() {
    const segments = location.pathname.split('/');
    if (segments.length >= 4) {
        console.log('version', segments[3])
        return segments[3];
    } else {
        console.log('default version __APPVERSION__')
        return '__APPVERSION__';
    }
}
function switchVersion(value) {
    if (!value) { return }
    this.location.href = location.protocol + '//' + location.host + '/infrastructures/dbrepo/' + value + '/';
}
document.getElementById("version").value = getVersion();
function getVersion() {
    const segments = location.pathname.split('/');
    if (segments.length >= 4) {
        return segments[3];
    } else {
        return '__APPVERSION__';
    }
}
function switchVersion(value) {
    if (!value) { return }
    this.location.href = location.protocol + '//' + location.host + '/infrastructures/dbrepo/' + value + '/';
}
document.getElementById("version").value = getVersion();
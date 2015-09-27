var map;
function initMap() {
    map = new google.maps.Map(document.getElementById('map'), {
        center: {
            lat: 44.4297378,
            lng: 26.1055353
        },
        zoom: 14
    });
}

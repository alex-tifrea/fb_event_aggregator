var toggleButton = document.querySelector("#toggleMenu");
var drawerPanel = document.querySelector('#paperDrawerPanel');
var narrow = drawerPanel.narrow;
var map;


drawerPanel.addEventListener("paper-responsive-change", function(e) {
  narrow = e.detail.narrow;
  if (narrow) {
    setTimeout(function () {
      google.maps.event.trigger(map, 'resize');
    }, 400);
  }
});

if (!narrow) {
  toggleButton.style.display = "none";
}

function toggleDrawer () {
  if (narrow) {
    toggleButton.style.display = "none";
    drawerPanel.responsiveWidth = "1px";
  } else {
    toggleButton.style.display = "block";
    drawerPanel.responsiveWidth = "9999px";
  }
}
document.toogleDrawer = toggleDrawer;

function initMap() {
  var mapDiv = document.querySelector('#map');
  map = new google.maps.Map(mapDiv, {
    center: {lat: -34.397, lng: 150.644},
    zoom: 8,
    disableDefaultUI: true
  });

   // Try HTML5 geolocation.
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
      var pos = {
        lat: position.coords.latitude,
        lng: position.coords.longitude
      };
      map.setCenter(pos);
      map.setZoom(11);
    }, function() {
      handleLocationError(true);
    });
  } else {
    // Browser doesn't support Geolocation
    handleLocationError(false);
  }
}

function handleLocationError(browserHasGeolocation) {
  if (browserHasGeolocation) {
    console.log('The Geolocation service failed.');
  }
  else {
    console.log('Error: Your browser doesn\'t support geolocation.');
  }
}

var toggleButton = document.querySelector("#toggleMenu");
var drawerPanel = document.querySelector('#paperDrawerPanel');
var narrow = drawerPanel.narrow;
var map = document.querySelector("google-maps");

drawerPanel.addEventListener("paper-responsive-change", function(e) {
  narrow = e.detail.narrow;
  var map = document.querySelector("google-map");
  if (narrow) {
    setTimeout(function () {
      map.resize();
    }, 500);
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
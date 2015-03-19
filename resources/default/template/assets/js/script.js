/*jslint browser: true*/
/*jslint unparam: true*/
/*global $, jQuery, hljs*/

$(document).ready(function($) {

  var NIGHT_THEME = getStyleReference("assets/css/vendor/obsidian.css");
  var LIGHT_THEME = getStyleReference("assets/css/vendor/github.css");
  var HEADINGS = $('#main_text h1, #main_text h2, #main_text h3, #main_text h4, #main_text h5, #main_text h6');
  var night = $("body").hasClass("night").toString();
  if (night !== localStorage.getItem("nuit")) { toggleNightMode(); }
  alernateThemes(NIGHT_THEME, LIGHT_THEME);
  hljs.initHighlightingOnLoad();

  // A faire dans le generateur de code, pas en JS !!
  $.each(HEADINGS, function(_, e) {
    var name = $(e).text().replace(/\s/g, '_').toLowerCase();
    $(e).attr('id', name);
  });

  $(HEADINGS).hover(function() {
    var text = "<span class='permalink'>Lien permanent</span>";
    $(this).append(text);
    setTimeout(function() {
      $('.permalink').addClass('appear');
    }, 600);
  }, function() {
    $('.permalink').remove();
  });

  $(HEADINGS).click(function() {
    goToTopOf($(this), $(this).attr('id'));
  });

  // Go to top of target element
  function goToTopOf(target, name) {
    $('html, body').animate({scrollTop: $(target).offset().top }, 300);
    name = name || "";
    document.location.hash = name;
  }

  // Go to next or previous title
  jQuery.fn.reverse = [].reverse;
  function goToNextHeading(dir) {
    var pos = $(document).scrollTop();
    var factor = dir === 'prev' ? -1 : 1;
    var headingsLocs = dir === 'prev' ? $(HEADINGS).reverse() : HEADINGS;

    headingsLocs.each(function(_, elem) {
      if ( factor * ($(elem).offset().top) > factor * (pos + factor) )
      {
        goToTopOf($(elem), $(elem).attr('id'));
        return false;
      }
    });
  }

  // Links smooth scroll
  $('a.smooth').click(function(e) {
    e.preventDefault();
    var target = $(this).attr('href');
    goToTopOf(target, $(this).attr('id'));
  });

  // Hide or show control buttons depending on position
  $(window).scroll(function(){
    var textPos = $("#main_text").offset().top;
    var footerPos = $("footer").offset().top;
    var thisPos = $(this).scrollTop();
    var opacity = ((thisPos > textPos - 100) && (thisPos < footerPos - 500)) ? 1 : 0;
    $('.botButton').css("opacity", opacity);
  });

  // Get light and dark themes indexes
  function getStyleReference(style) {
    var styleReference;

    $.each(document.styleSheets, function(_, e) {
      if (e.href.indexOf(style) >= 0)
      {
        styleReference = e;
        return false;
      }
    });
    return styleReference;
  }

  function alernateThemes(theme1, theme2)
  {
    if (!night)
    {
      theme1.disabled = false;
      theme2.disabled = true;
    }
    else
    {
      theme1.disabled = true;
      theme2.disabled = false;
    }
  }

  // Switch to night or normal mode
  function toggleNightMode(e) {
    if (e) { e.preventDefault(); }
    $("body").toggleClass('night');
    night = !night;
    $(".nightButton").toggleClass('pushed');
    NIGHT_THEME.disabled = !NIGHT_THEME.disabled;
    LIGHT_THEME.disabled = !LIGHT_THEME.disabled;
  }

  function setNightPreference()
  {
    localStorage.setItem("nuit", !night);
  }
  $(".nightButton").click(toggleNightMode);
  $(".nightButton").click(setNightPreference);

  // Automatically set night mode if it is nighttime
  var currentHour = (new Date()).getHours();
  var userNightPreference = localStorage.getItem("nuit");
  if (userNightPreference === "null" && (currentHour >= 21 || currentHour <= 8)) { toggleNightMode(); }

  // Keybindings
  $(document).keyup(function(e) {
    e.preventDefault();
    var car = String.fromCharCode(e.which).toLowerCase();
    switch (car)
    {
      case 'n':
      toggleNightMode();
      setNightPreference();
      break;
      case 'g':
      if ($(this).scrollTop() >= ($('#main_text').offset().top - 10))
      {
        goToTopOf($('#top'));
      }
      else
      {
        goToTopOf($('#main_text'));
      }
      break;
      case 'l': case "'":
      goToNextHeading('next');
      break;
      case 'h': case '%':
      goToNextHeading('prev');
      break;
    }
  });

});

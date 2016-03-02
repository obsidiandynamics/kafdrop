jQuery(document).ready(function(){
   jQuery(document).on('click', '.toggle-link', function(e) {
      var self = jQuery(this),
          linkText = self.find("i"),
          target = jQuery(document).find(self.data('toggle-target'));

      e.preventDefault();
      target.slideToggle();
      linkText.toggleClass('fa-chevron-circle-down fa-chevron-circle-right');
   });
});
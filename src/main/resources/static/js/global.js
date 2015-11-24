jQuery(document).ready(function(){
   jQuery(document).on('click', '.toggle-link', function(e) {
      var self = jQuery(this),
          linkText = self.text(),
          target = jQuery(document).find(self.data('toggle-target'));

      e.preventDefault();
      target.toggle();

      if (linkText == '+') self.text('-');
      else if (linkText == '-') self.text('+');
   });
});
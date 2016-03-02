jQuery(document).ready(function() {
    jQuery(document).on('click', '.toggle-msg', function(e) {
        var link=jQuery(this),
            linkIcon=link.find('.fa'),
            body=link.parent().find('.message-body');

        e.preventDefault();

        linkIcon.toggleClass('fa-chevron-circle-right fa-chevron-circle-down')
        if (true == body.data('expanded')) {
            body.text(JSON.stringify(JSON.parse(body.text())));
            body.data('expanded', false);
        }
        else {
            body.text(JSON.stringify(JSON.parse(body.text()), null, 3));
            body.data('expanded', true);
        }
    });

    jQuery(document).on('change', '#partition', function(e) {
        var selectedOption = jQuery(this).children("option").filter(":selected"),
            firstOffset = selectedOption.data('firstOffset'),
            lastOffset = selectedOption.data('lastOffset');
        jQuery('#firstOffset').text(firstOffset);
        jQuery('#lastOffset').text(lastOffset);
        jQuery('#partitionSize').text(lastOffset - firstOffset)
    });
})

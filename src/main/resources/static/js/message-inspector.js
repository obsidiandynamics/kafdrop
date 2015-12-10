jQuery(document).ready(function() {
    jQuery(document).on('dblclick', '.message-detail pre', function(e) {
        var body=jQuery(this)

        e.preventDefault();

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
        var selectedOption = jQuery(this).children("option").filter(":selected");
        var firstOffset = selectedOption.data('firstOffset');
        var lastOffset = selectedOption.data('lastOffset');
        jQuery('#firstOffset').text(firstOffset);
        jQuery('#lastOffset').text(lastOffset);
        jQuery('#partitionSize').text(lastOffset - firstOffset)
    });
})

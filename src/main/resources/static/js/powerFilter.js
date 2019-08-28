/*
 * Copyright 2016 Kafdrop contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *
     <INPUT id='filter' size=25 NAME='searchRow' title='Just type to filter the rows'>
     &nbsp;
     <span id="rowCount"></span>

     <table>
        <iterate>
            <tr class="dataRow">
 *
 *
 */


jQuery.expr[':'].contains = function (a, i, m) {
    return jQuery(a).text().toUpperCase().indexOf(m[3].toUpperCase()) >= 0;
};

function prefix(text, pref) {
    return text.trim().length == 0 ? "" : pref;
}

$(document).ready(function () {
    $('input[name="searchRow"]').attr('title', "Power Filter: w/space for OR, '!' for exclude");

    var counter = "(" + $('.dataRow:visible').length + ")";
    $('#rowCount').text(counter);

    $('input[name="searchRow"]').keyup(function () {
        var searchterm = $(this).val();
        var rowTag = "tr.dataRow";
        if (searchterm.length >= 2) {
            searchterm = searchterm.replace(/\s+/g, ' ');
            searchterm = searchterm.replace(/!\s+/g, '!');
            searchterm = $.trim(searchterm);
            var bits = searchterm.split(" ");

            var matchExpr = "";
            var blockExpr = "";
            var andExpr = "";
            var noMatchExpr = rowTag;
            var someFilter = false;

            for (var i = 0; i < bits.length; i++) {
                var bit = bits[i];

                if (bit == "!") {
                    continue;
                }

                if (bit.slice(0, 1) == '!') {
                    var ignore = bit.substring(1);
                    if (ignore.length == 1) {
                        continue;
                    }
                    blockExpr += prefix(blockExpr, ",") + rowTag + ':contains("' + ignore + '")';
                } else if (bit.slice(0, 1) == '+') {
                    var ignore = bit.substring(1);
                    if (ignore.length == 1) {
                        continue;
                    }
                    andExpr += prefix(andExpr, ",") + rowTag + ':not(:contains("' + ignore + '"))';
                } else {
                    matchExpr += prefix(matchExpr, ",") + rowTag + ':contains("' + bit + '")';
                    noMatchExpr += ':not(:contains("' + bit + '"))';
                    someFilter = true;
                }
            }

            //$('#debug').text('blockExpr (' + blockExpr + ')     matchExpr(' + matchExpr + ")"  + '    noMatchExpr(' + noMatchExpr + ")");

            var match = $(matchExpr);
            var nomatch = $(noMatchExpr);
            var blocker = $(blockExpr);
            var ander = $(andExpr);

            if (someFilter) {
                nomatch.css("display", "none");
            }

            match.css("display", "");
            blocker.css("display", "none");
            ander.css("display", "  none");

            if (searchterm.length >= 10) {
                $(this).css("width", "200px");
            }
        } else {
            $(rowTag).css("display", "");
            $(rowTag).removeClass('selected');
        }

        var label = "(" + $('.dataRow:visible').length + ")";
        $('#rowCount').text(label);


    });

    var curr = $('#filter').val();
    if (curr != '') {
        $('#filter').keyup();
    }
});


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
 */

jQuery(document).ready(function () {
    jQuery(document).on('click', '.toggle-link', function (e) {
        var self = jQuery(this),
            linkText = self.find("i"),
            target = jQuery(document).find(self.data('toggle-target'));

        e.preventDefault();
        target.slideToggle();
        linkText.toggleClass('fa-chevron-circle-down fa-chevron-circle-right');
    });
});
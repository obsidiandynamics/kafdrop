<#--
 Copyright 2021 Kafdrop contributors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<#import "/spring.ftl" as spring />

<div class="pb-2 mt-5 mb-4 border-bottom border-secondary">
    <div class="container">
        <div class="container-fluid pl-0">
            <div id="header-title-line" class="row">
                <div id="logo" class="col-md-1">
                    <img alt="logo" height="100%" src="<@spring.url '/images/kafdrop-logo.svg'/>"/>
                </div>
                <div id="title" class="col-md-10">
                    <h1 class="app-name brand mb-0">
                        <a href="<@spring.url '/'/>">Kafdrop</a>
                    </h1>
                </div>
                <div id="github-star" class="col-md-1">
                    <a class="github-button" href="https://github.com/obsidiandynamics/kafdrop" data-show-count="false"
                       aria-label="Star Kafdrop on GitHub" data-color-scheme="dark">Star</a>
                </div>
                <script>
                    $(document).ready(function(){
                        setTimeout(function() { restyle(0); });
                    });

                    function restyle(retries) {
                        var githubStarSpan = document.querySelector('#github-star span');
                        if (githubStarSpan != null) {
                            var shadowRoot = githubStarSpan.shadowRoot;
                            shadowRoot.querySelector('.btn')
                                .setAttribute('style', 'color:#00f0fe; background-image:none; background-color:#222; border-color:#222');
                            shadowRoot.querySelector('.social-count')
                                .setAttribute('style', 'color:#222; background-image:none; background-color:#00f0fe; border-color:#00f0fe');
                            shadowRoot.querySelector('.social-count b')
                                .setAttribute('style', 'border-right-color:#00f0fe');
                            shadowRoot.querySelector('.social-count i')
                                .setAttribute('style', 'border-right-color:#00f0fe');
                        } else {
                            setTimeout(function() { restyle(retries + 1); }, retries * 10);
                        }
                    }
                </script>
            </div>
        </div>
    </div>
</div>

<#--
 Copyright 2016 Kafdrop contributors.

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

<div class="pb-2 mt-5 mb-4 border-bottom">
    <div class="container">
        <div class="container-fluid pl-0">
            <div class="row">
                <div id="title" class="col-md-11">
                    <h1 class="app-name brand"><a href="<@spring.url '/'/>">Kafdrop</a> <#if profile??><span class="small">${profile}</span></#if>
                    </h1>
                </div>
                <div id="github-star" class="col-md-1">
                    <a class="github-button" href="https://github.com/obsidiandynamics/kafdrop" data-show-count="true"
                       aria-label="Star Kafdrop on GitHub">Star</a>
                </div>
                <script>
                    $(document).ready(function(){
                        setTimeout(restyle);
                    });

                    function restyle() {
                        var githubStarSpan = document.querySelector('#github-star span');
                        if (githubStarSpan != null) {
                            var shadowRoot = githubStarSpan.shadowRoot;
                            shadowRoot.querySelector('.btn')
                                .setAttribute('style', 'color:#00f0fe; background-image:none; background-color:#222');
                            shadowRoot.querySelector('.social-count')
                                .setAttribute('style', 'color:#222; background-image:none; background-color:#00f0fe; border-color:#00f0fe');
                            shadowRoot.querySelector('.social-count b')
                                .setAttribute('style', 'border-right-color:#00f0fe');
                            shadowRoot.querySelector('.social-count i')
                                .setAttribute('style', 'border-right-color:#00f0fe');
                        } else {
                            setTimeout(restyle);
                        }
                    }
                </script>
            </div>
        </div>
    </div>
</div>

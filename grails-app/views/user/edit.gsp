<html>
    <head>
        <meta name="layout" content="main"/>
        <title>Edit</title>
    </head>
    <body>            
        <div id="tabs" class="ui-tabs">
            <ul>
                <li><g:link title="Account Tab" action="showTab" id="${user.id}" params="[tab:'account', conversationId: conversation.id]">Account</g:link></li>
                <li><g:link title="Profile Tab" action="showTab" id="${user.id}" params="[tab:'profile', conversationId: conversation.id]">Profile</g:link></li>
            </ul>
            <div id="Account_Tab" class="ui-tabs-hide"></div>
            <div id="Profile_Tab" class="ui-tabs-hide"></div>            
        </div>
        <g:javascript>
            jQuery(function() {
                var userTabs = jQuery("#tabs");
                userTabs.tabs({
                    select: function(event, ui) {
                        var oldTabIndex = userTabs.tabs('option', 'selected');
                        var currentTab = jQuery('.ui-tabs-panel', userTabs)[oldTabIndex];
                        SPC.autoSave(currentTab)
                    }                    
                });
            });
        </g:javascript>
    </body>
</html>

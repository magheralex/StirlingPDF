document.addEventListener('DOMContentLoaded', function() {
	setLanguageForDropdown('.lang_dropdown-item');
	const defaultLocale = document.documentElement.lang || 'en_GB';
	const storedLocale = localStorage.getItem('languageCode') || defaultLocale;
	const dropdownItems = document.querySelectorAll('.lang_dropdown-item');

	for (let i = 0; i < dropdownItems.length; i++) {
		const item = dropdownItems[i];
		item.classList.remove('active');
		if (item.dataset.languageCode === storedLocale) {
			item.classList.add('active');
		}
		item.addEventListener('click', handleDropdownItemClick);
	}
});

function setLanguageForDropdown(dropdownClass) {
    const defaultLocale = document.documentElement.lang || 'en_GB';
    const storedLocale = localStorage.getItem('languageCode') || defaultLocale;
    const dropdownItems = document.querySelectorAll(dropdownClass);

    for (let i = 0; i < dropdownItems.length; i++) {
        const item = dropdownItems[i];
        item.classList.remove('active');
        if (item.dataset.languageCode === storedLocale) {
            item.classList.add('active');
        }
        item.addEventListener('click', handleDropdownItemClick);
    }
}

function handleDropdownItemClick(event) {
    event.preventDefault();
    const languageCode = event.currentTarget.dataset.bsLanguageCode;  // change this to event.currentTarget
    if (languageCode) {
        localStorage.setItem('languageCode', languageCode);

        const currentUrl = window.location.href;
        if (currentUrl.indexOf('?lang=') === -1) {
            window.location.href = currentUrl + '?lang=' + languageCode;
        } else {
            window.location.href = currentUrl.replace(/\?lang=\w{2,}/, '?lang=' + languageCode);
        }
    } else {
        console.error("Language code is not set for this item.");  // for debugging
    }
}


document.addEventListener('DOMContentLoaded', function() {
		document.querySelectorAll('.nav-item.dropdown').forEach((element) => {
	    const dropdownMenu = element.querySelector(".dropdown-menu");
	    if (dropdownMenu.id !== 'favoritesDropdown' &&  dropdownMenu.children.length <= 2 && dropdownMenu.querySelectorAll("hr.dropdown-divider").length === dropdownMenu.children.length) {
	        if (element.previousElementSibling && element.previousElementSibling.classList.contains('nav-item') && element.previousElementSibling.classList.contains('nav-item-separator')) {
	            element.previousElementSibling.remove();
	        }
	        element.remove();
	    }
	});
	
	//Sort languages by alphabet
	const list = Array.from(document.querySelector('.dropdown-menu[aria-labelledby="languageDropdown"]').children).filter(child => child.matches('a'));
	list.sort(function(a, b) {
	    var A = a.textContent.toUpperCase();
	    var B = b.textContent.toUpperCase();
	    return (A < B) ? -1 : (A > B) ? 1 : 0;
	}).forEach(node => document.querySelector('.dropdown-menu[aria-labelledby="languageDropdown"]').appendChild(node));

});
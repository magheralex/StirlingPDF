document.getElementById('validateButton').addEventListener('click', function(event) {
	event.preventDefault();
	validatePipeline();
});
function validatePipeline() {
	let pipelineListItems = document.getElementById('pipelineList').children;
	let isValid = true;
	let containsAddPassword = false;
	for (let i = 0; i < pipelineListItems.length - 1; i++) {
		let currentOperation = pipelineListItems[i].querySelector('.operationName').textContent;
		let nextOperation = pipelineListItems[i + 1].querySelector('.operationName').textContent;
		if (currentOperation === '/add-password') {
			containsAddPassword = true;
		}
		console.log(currentOperation);
		console.log(apiDocs[currentOperation]);
		let currentOperationDescription = apiDocs[currentOperation]?.post?.description || "";
		let nextOperationDescription = apiDocs[nextOperation]?.post?.description || "";

		console.log("currentOperationDescription", currentOperationDescription);
		console.log("nextOperationDescription", nextOperationDescription);

		let currentOperationOutput = currentOperationDescription.match(/Output:([A-Z\/]*)/)?.[1] || "";
		let nextOperationInput = nextOperationDescription.match(/Input:([A-Z\/]*)/)?.[1] || "";

		console.log("Operation " + currentOperation + " Output: " + currentOperationOutput);
		console.log("Operation " + nextOperation + " Input: " + nextOperationInput);

		// Splitting in case of multiple possible output/input
		let currentOperationOutputArr = currentOperationOutput.split('/');
		let nextOperationInputArr = nextOperationInput.split('/');

		if (currentOperationOutput !== 'ANY' && nextOperationInput !== 'ANY') {
			let intersection = currentOperationOutputArr.filter(value => nextOperationInputArr.includes(value));
			console.log(`Intersection: ${intersection}`);

			if (intersection.length === 0) {
				isValid = false;
				console.log(`Incompatible operations: The output of operation '${currentOperation}' (${currentOperationOutput}) is not compatible with the input of the following operation '${nextOperation}' (${nextOperationInput}).`);
				alert(`Incompatible operations: The output of operation '${currentOperation}' (${currentOperationOutput}) is not compatible with the input of the following operation '${nextOperation}' (${nextOperationInput}).`);
				break;
			}
		}
	}
	if (containsAddPassword && pipelineListItems[pipelineListItems.length - 1].querySelector('.operationName').textContent !== '/add-password') {
		alert('The "add-password" operation should be at the end of the operations sequence. Please adjust the operations order.');
		return false;
	}
	if (isValid) {
		console.log('Pipeline is valid');
		// Continue with the pipeline operation
	} else {
		console.error('Pipeline is not valid');
		// Stop operation, maybe display an error to the user
	}

	return isValid;
}





document.getElementById('submitConfigBtn').addEventListener('click', function() {

	if (validatePipeline() === false) {
		return;
	}
	let selectedOperation = document.getElementById('operationsDropdown').value;
	let parameters = operationSettings[selectedOperation] || {};

	let pipelineConfig = {
		"name": "uniquePipelineName",
		"pipeline": [{
			"operation": selectedOperation,
			"parameters": parameters
		}],
		"_examples": {
			"outputDir": "{outputFolder}/{folderName}",
			"outputFileName": "{filename}-{pipelineName}-{date}-{time}"
		},
		"outputDir": "httpWebRequest",
		"outputFileName": "{filename}"
	};

	let pipelineConfigJson = JSON.stringify(pipelineConfig, null, 2);

	let formData = new FormData();

	let fileInput = document.getElementById('fileInput-input');
	let files = fileInput.files;

	for (let i = 0; i < files.length; i++) {
		console.log("files[i]", files[i].name);
		formData.append('fileInput', files[i], files[i].name);
	}

	console.log("pipelineConfigJson", pipelineConfigJson);
	formData.append('json', pipelineConfigJson);
	console.log("formData", formData);

	fetch('/handleData', {
		method: 'POST',
		body: formData
	})
		.then(response => response.blob())
		.then(blob => {

			let url = window.URL.createObjectURL(blob);
			let a = document.createElement('a');
			a.href = url;
			a.download = 'outputfile';
			document.body.appendChild(a);
			a.click();
			a.remove();
		})
		.catch((error) => {
			console.error('Error:', error);
		});
});

let apiDocs = {};

let operationSettings = {};

fetch('v3/api-docs')
	.then(response => response.json())
	.then(data => {

		apiDocs = data.paths;
		let operationsDropdown = document.getElementById('operationsDropdown');
		const ignoreOperations = ["/handleData", "operationToIgnore"]; // Add the operations you want to ignore here

		operationsDropdown.innerHTML = '';

		let operationsByTag = {};

		// Group operations by tags
		Object.keys(data.paths).forEach(operationPath => {
			let operation = data.paths[operationPath].post;
			if (operation && !ignoreOperations.includes(operationPath) && !operation.description.includes("Type:MISO")) {
				let operationTag = operation.tags[0]; // This assumes each operation has exactly one tag
				if (!operationsByTag[operationTag]) {
					operationsByTag[operationTag] = [];
				}
				operationsByTag[operationTag].push(operationPath);
			}
		});

		// Specify the order of tags
		let tagOrder = ["General", "Security", "Convert", "Other", "Filter"];

		// Create dropdown options
		tagOrder.forEach(tag => {
			if (operationsByTag[tag]) {
				let group = document.createElement('optgroup');
				group.label = tag;

				operationsByTag[tag].forEach(operationPath => {
					let option = document.createElement('option');
					let operationWithoutSlash = operationPath.replace(/\//g, ''); // Remove slashes
					option.textContent = operationWithoutSlash;
					option.value = operationPath; // Keep the value with slashes for querying
					group.appendChild(option);
				});

				operationsDropdown.appendChild(group);
			}
		});
	});


document.getElementById('addOperationBtn').addEventListener('click', function() {
	let selectedOperation = document.getElementById('operationsDropdown').value;
	let pipelineList = document.getElementById('pipelineList');

	let listItem = document.createElement('li');
	listItem.className = "list-group-item";
	let hasSettings = (apiDocs[selectedOperation] && apiDocs[selectedOperation].post &&
		((apiDocs[selectedOperation].post.parameters && apiDocs[selectedOperation].post.parameters.length > 0) ||
			(apiDocs[selectedOperation].post.requestBody &&
				apiDocs[selectedOperation].post.requestBody.content['multipart/form-data'].schema.properties)));




	listItem.innerHTML = `
		<div class="d-flex justify-content-between align-items-center w-100">
			<div class="operationName">${selectedOperation}</div>
			<div class="arrows d-flex">
				<button class="btn btn-secondary move-up btn-margin"><span>&uarr;</span></button>
				<button class="btn btn-secondary move-down btn-margin"><span>&darr;</span></button>
				<button class="btn btn-warning pipelineSettings btn-margin" ${hasSettings ? "" : "disabled"}><span style="color: ${hasSettings ? "black" : "grey"};">⚙️</span></button>
				<button class="btn btn-danger remove"><span>X</span></button>
			</div>
		</div>
	`;

	pipelineList.appendChild(listItem);

	listItem.querySelector('.move-up').addEventListener('click', function(event) {
		event.preventDefault();
		if (listItem.previousElementSibling) {
			pipelineList.insertBefore(listItem, listItem.previousElementSibling);
		}
	});

	listItem.querySelector('.move-down').addEventListener('click', function(event) {
		event.preventDefault();
		if (listItem.nextElementSibling) {
			pipelineList.insertBefore(listItem.nextElementSibling, listItem);
		}
	});

	listItem.querySelector('.remove').addEventListener('click', function(event) {
		event.preventDefault();
		pipelineList.removeChild(listItem);
	});

	listItem.querySelector('.pipelineSettings').addEventListener('click', function(event) {
		event.preventDefault();
		showpipelineSettingsModal(selectedOperation);
	});

	function showpipelineSettingsModal(operation) {
		let pipelineSettingsModal = document.getElementById('pipelineSettingsModal');
		let pipelineSettingsContent = document.getElementById('pipelineSettingsContent');
		let operationData = apiDocs[operation].post.parameters || [];
		let requestBodyData = apiDocs[operation].post.requestBody.content['multipart/form-data'].schema.properties || {};

		// Combine operationData and requestBodyData into a single array
		operationData = operationData.concat(Object.keys(requestBodyData).map(key => ({
			name: key,
			schema: requestBodyData[key]
		})));

		pipelineSettingsContent.innerHTML = '';

		operationData.forEach(parameter => {
			// If the parameter name is 'fileInput', return early to skip the rest of this iteration
    		if (parameter.name === 'fileInput') return;
    
			let parameterDiv = document.createElement('div');
			parameterDiv.className = "mb-3";

			let parameterLabel = document.createElement('label');
			parameterLabel.textContent = `${parameter.name} (${parameter.schema.type}): `;
			parameterLabel.title = parameter.description;
			parameterDiv.appendChild(parameterLabel);

			let parameterInput;

			// check if enum exists in schema
			if (parameter.schema.enum) {
				// if enum exists, create a select element
				parameterInput = document.createElement('select');
				parameterInput.className = "form-control";

				// iterate over each enum value and create an option for it
				parameter.schema.enum.forEach(value => {
					let option = document.createElement('option');
					option.value = value;
					option.text = value;
					parameterInput.appendChild(option);
				});
			} else {
				// switch-case statement for handling non-enum types
				switch (parameter.schema.type) {
					case 'string':
						if (parameter.schema.format === 'binary') {
							// This is a file input
							
							//parameterInput = document.createElement('input');
							//parameterInput.type = 'file';
							//parameterInput.className = "form-control";
							
							parameterInput = document.createElement('input');
							parameterInput.type = 'text';
							parameterInput.className = "form-control";
							parameterInput.value = "automatedFileInput";
						} else {
							parameterInput = document.createElement('input');
							parameterInput.type = 'text';
							parameterInput.className = "form-control";
						}
						break;
					case 'number':
					case 'integer':
						parameterInput = document.createElement('input');
						parameterInput.type = 'number';
						parameterInput.className = "form-control";
						break;
					case 'boolean':
						parameterInput = document.createElement('input');
						parameterInput.type = 'checkbox';
						break;
					case 'array':
					case 'object':
						parameterInput = document.createElement('textarea');
						parameterInput.placeholder = `Enter a JSON formatted ${parameter.schema.type}`;
						parameterInput.className = "form-control";
						break;
					default:
						parameterInput = document.createElement('input');
						parameterInput.type = 'text';
						parameterInput.className = "form-control";
				}
			}
			parameterInput.id = parameter.name;

			if (operationSettings[operation] && operationSettings[operation][parameter.name] !== undefined) {
				let savedValue = operationSettings[operation][parameter.name];

				switch (parameter.schema.type) {
					case 'number':
					case 'integer':
						parameterInput.value = savedValue.toString();
						break;
					case 'boolean':
						parameterInput.checked = savedValue;
						break;
					case 'array':
					case 'object':
						parameterInput.value = JSON.stringify(savedValue);
						break;
					default:
						parameterInput.value = savedValue;
				}
			}

			parameterDiv.appendChild(parameterInput);

			pipelineSettingsContent.appendChild(parameterDiv);
		});

		let saveButton = document.createElement('button');
		saveButton.textContent = "Save Settings";
		saveButton.className = "btn btn-primary";
		saveButton.addEventListener('click', function(event) {
			event.preventDefault();
			let settings = {};
			operationData.forEach(parameter => {
				let value = document.getElementById(parameter.name).value;
				switch (parameter.schema.type) {
					case 'number':
					case 'integer':
						settings[parameter.name] = Number(value);
						break;
					case 'boolean':
						settings[parameter.name] = document.getElementById(parameter.name).checked;
						break;
					case 'array':
					case 'object':
						try {
							settings[parameter.name] = JSON.parse(value);
						} catch (err) {
							console.error(`Invalid JSON format for ${parameter.name}`);
						}
						break;
					default:
						settings[parameter.name] = value;
				}
			});
			operationSettings[operation] = settings;
			console.log(settings);
			pipelineSettingsModal.style.display = "none";
		});
		pipelineSettingsContent.appendChild(saveButton);

		pipelineSettingsModal.style.display = "block";

		pipelineSettingsModal.getElementsByClassName("close")[0].onclick = function() {
			pipelineSettingsModal.style.display = "none";
		}

		window.onclick = function(event) {
			if (event.target == pipelineSettingsModal) {
				pipelineSettingsModal.style.display = "none";
			}
		}
	}

	document.getElementById('savePipelineBtn').addEventListener('click', function() {
		if (validatePipeline() === false) {
			return;
		}
		var pipelineName = document.getElementById('pipelineName').value;
		let pipelineList = document.getElementById('pipelineList').children;
		let pipelineConfig = {
			"name": pipelineName,
			"pipeline": [],
			"_examples": {
				"outputDir": "{outputFolder}/{folderName}",
				"outputFileName": "{filename}-{pipelineName}-{date}-{time}"
			},
			"outputDir": "httpWebRequest",
			"outputFileName": "{filename}"
		};

		for (let i = 0; i < pipelineList.length; i++) {
			let operationName = pipelineList[i].querySelector('.operationName').textContent;
			let parameters = operationSettings[operationName] || {};

			pipelineConfig.pipeline.push({
				"operation": operationName,
				"parameters": parameters
			});
		}

		let a = document.createElement('a');
		a.href = URL.createObjectURL(new Blob([JSON.stringify(pipelineConfig, null, 2)], {
			type: 'application/json'
		}));
		a.download = 'pipelineConfig.json';
		a.style.display = 'none';

		document.body.appendChild(a);
		a.click();
		document.body.removeChild(a);
	});

	async function processPipelineConfig(configString) {
		let pipelineConfig = JSON.parse(configString);
		let pipelineList = document.getElementById('pipelineList');

		while (pipelineList.firstChild) {
			pipelineList.removeChild(pipelineList.firstChild);
		}
		document.getElementById('pipelineName').value = pipelineConfig.name
		for (const operationConfig of pipelineConfig.pipeline) {
			let operationsDropdown = document.getElementById('operationsDropdown');
			operationsDropdown.value = operationConfig.operation;
			operationSettings[operationConfig.operation] = operationConfig.parameters;

			// assuming addOperation is async
			await new Promise((resolve) => {
				document.getElementById('addOperationBtn').addEventListener('click', resolve, { once: true });
				document.getElementById('addOperationBtn').click();
			});

			let lastOperation = pipelineList.lastChild;

			Object.keys(operationConfig.parameters).forEach(parameterName => {
				let input = document.getElementById(parameterName);
				if (input) {
					switch (input.type) {
						case 'checkbox':
							input.checked = operationConfig.parameters[parameterName];
							break;
						case 'number':
							input.value = operationConfig.parameters[parameterName].toString();
							break;
						case 'file':
							if (parameterName !== 'fileInput') {
								// Create a new file input element
								let newInput = document.createElement('input');
								newInput.type = 'file';
								newInput.id = parameterName;

								// Add the new file input to the main page (change the selector according to your needs)
								document.querySelector('#main').appendChild(newInput);
							}
							break;
						case 'text':
						case 'textarea':
						default:
							input.value = JSON.stringify(operationConfig.parameters[parameterName]);
					}
				}
			});

		}
	}


	document.getElementById('uploadPipelineBtn').addEventListener('click', function() {
		document.getElementById('uploadPipelineInput').click();
	});

	document.getElementById('uploadPipelineInput').addEventListener('change', function(e) {
		let reader = new FileReader();
		reader.onload = function(event) {
			processPipelineConfig(event.target.result);
		};
		reader.readAsText(e.target.files[0]);
	});

	document.getElementById('pipelineSelect').addEventListener('change', function(e) {
		let selectedPipelineJson = e.target.value;  // assuming the selected value is the JSON string of the pipeline config
		processPipelineConfig(selectedPipelineJson);
	});


});
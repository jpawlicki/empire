const isValidElement = element => {
  return element.name && element.value;
};
const isValidValue = element => {
  return (!['radio'].includes(element.type) || element.checked);
};
const isCheckbox = element => element.type === 'checkbox';
const isMultiSelect = element => element.options && element.multiple;
const getSelectValues = options => [].reduce.call(options, (values, option) => {
  return option.selected ? values.concat(option.value) : values;
}, []);

function formToJSON(elements) {
  return [].reduce.call(elements, (data, element) => {
		if (isValidElement(element) && isValidValue(element)) {
			if (isCheckbox(element)) {
				if (element.checked) data[element.name] = "checked";
				else data[element.name] = "unchecked";
			} else if (isMultiSelect(element)) {
				data[element.name] = getSelectValues(element);
			} else {
				data[element.name] = element.value;
			}
		}
		return data;
	}, {});
}

function formToJson(formid) {
  return JSON.stringify(formToJSON(document.getElementById(formid).elements));
};

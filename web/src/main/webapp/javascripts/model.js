function submitModel(modelAttributes,dynamicAttributes){
	if(modelAttributes==true){
		var modelAttributes = {};
		$("#modelAttributesTable input").each(function(){
			var key = $(this).attr("id");
			var value = $(this).val();
			if(value!=null&&value!=''){
				value = value.toUpperCase();
			}
			modelAttributes[key]=value;
		});
		$("#modelAttributesTable select").each(function(){
			var key = $(this).attr("id");
			var value = $(this).val();
			if(value!=null&&value!=''){
				value = value.toUpperCase();
			}
			modelAttributes[key]=value;
		});
		var modelAttributesJSON = JSON.stringify(modelAttributes);
		$("#modelAttributesJSON").val(modelAttributesJSON);
	}
	if(dynamicAttributes==true){
		var dynamicAttributes = {};
		$("#dynamicAttributesTable select").each(function(){
			var key = $(this).attr("id");
			var value = $(this).val();
			dynamicAttributes[key]=value;
		});
		$("#dynamicAttributesTable input").each(function(){
			var key = $(this).attr("id");
			var value = $(this).val();
			dynamicAttributes[key]=value;
		});
		var dynamicAttributesJSON = JSON.stringify(dynamicAttributes);
		$("#dynamicAttributesJSON").val(dynamicAttributesJSON);
	}
	return true;
}

function searchModel(){
	var searchFilters = {};
	$("#searchFilterTable input[type=text]").each(function(){
		var key = $(this).attr("id");
		var value = $(this).val();
		if(value!=null&&value!='')
			{
			searchFilters[key]=value;
			}
	});
	$("#searchFilterTable select").each(function(){
		var key = $(this).attr("id");
		var value = $(this).val();
		if(value!=null&&value!='')
			{
			searchFilters[key]=value;
			}
	});
	var searchFiltersJSON = JSON.stringify(searchFilters);
	$("#searchAttributesJSON").val(searchFiltersJSON);
	$("#searchForm").submit();
}

function toggleAll(){
	var checked = $("#selectAllChildren").is(':checked');
    if (checked) {
        $('[id=selectChildrenList]:checkbox').prop('checked',true);
    } else {
        $('[id=selectChildrenList]:checkbox').prop('checked', false);
    }       
}
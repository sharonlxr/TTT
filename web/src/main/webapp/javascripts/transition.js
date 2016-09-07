function validateRequiredfield(prefix,formId) {		
	var elementsForms = document.getElementById(formId);
		for (var intCounter = 0; intCounter < elementsForms.length; intCounter++)
		{
		 if	(strStartsWith(elementsForms[intCounter].id,prefix) ||
			 strStartsWith(elementsForms[intCounter].id,"required_") )
			 {
			 var tmpobj = elementsForms[intCounter];
			 var strfield = elementsForms[intCounter].value;
			 if (tmpobj.type =="select-one" ||tmpobj.type =="select-multiple") 
			  	{		
				  if (tmpobj.selectedIndex==-1) {
					  strfield = null;					  
				  }
				  else {
					  strfield = tmpobj.options[tmpobj.selectedIndex].value;
				  }				  				
			  	}
			  if (strfield == "" || strfield == null ||strfield.charAt(0) == ' ')
				  {				   
				   alert("Please fill in " + elementsForms[intCounter].title);
				   return false;
				  }
			 
			 }     		      			
		}
		return true;     		     		     		            
}

function strStartsWith(str, prefix) {    
	return str.indexOf(prefix) === 0;
	}

function validOption()
    {
	var forced = document.getElementById('-Non-defined transitions-');
	var empty  = document.getElementById(' ');
	if( (forced != null && forced.selected) || (empty != null && empty.selected) )
	    {
		alert('Select a State');
		return false;
		}
	return true;
    }


function forceTransition()
    {
	var sel = document.getElementById('substate');
	var opt=sel.options[sel.selectedIndex];
	if(! strStartsWith(opt.id,'forced_')) {return true;}
	return confirm('This transition is not defined, Do you want to force the transition?');
    }

function validateTransaction(prefix,formId)
   {
	if(! validOption())	{return false;}
	if(!forceTransition()){return false;}
	return validateRequiredfield(prefix,formId);
   }

function onchangestate(dropdown) {
	var myindex  = dropdown.selectedIndex;
    var SelValue = dropdown.options[myindex].value;
	var arraytemp = new Array(); 
	arraytemp = window.location.href.split('&');		
	window.location.href = arraytemp[0] +  '&toSubstate=' + SelValue; 
	return true;	
}

if (!Array.prototype.indexOf) {
	  Array.prototype.indexOf = function (searchElement /*, fromIndex */ ) {
	    'use strict';
	    if (this == null) {
	      throw new TypeError();
	    }
	    var n, k, t = Object(this),
	        len = t.length >>> 0;

	    if (len === 0) {
	      return -1;
	    }
	    n = 0;
	    if (arguments.length > 1) {
	      n = Number(arguments[1]);
	      if (n != n) { // shortcut for verifying if it's NaN
	        n = 0;
	      } else if (n != 0 && n != Infinity && n != -Infinity) {
	        n = (n > 0 || -1) * Math.floor(Math.abs(n));
	      }
	    }
	    if (n >= len) {
	      return -1;
	    }
	    for (k = n >= 0 ? n : Math.max(len - Math.abs(n), 0); k < len; k++) {
	      if (k in t && t[k] === searchElement) {
	        return k;
	      }
	    }
	    return -1;
	  };
	}
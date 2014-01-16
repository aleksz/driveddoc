
/*
*  Javascripti klienditeegi versioon 0.14
*  Käesoleva javascripti klienditeegi dokumentatsiooni levitatakse eraldi dokumendina "Veebis signeerimise Javascripti klienditeek".
*  Dokumentatsiooni saamiseks ja muude küsimuste korral pöörduda abi@id.ee
*  
*  Muudatuste ajalugu:
*
*  versioon 0.14 mai 2013
*  	- eemaldatud kõigi vanade plugin-ide tugi, jäetud alles ainult uue id-kaardi baastarkvara
*  	plugin-i (application/x-digidoc) tugi.
*   - Plugina vaikimisi keeleks on inglise keel.
*	- Uuendatud/muudetud tõlkeid
*  
*  versioon 0.13 mai 2013
*   - Vana Mac OSX plugin-i (MIME tüüp application/x-idcard-plugin) toe kadumine
*
*  versioon 0.12 14. märts 2012
*	- Java appleti toe ärakadumisega seoses muudatus: kui allkirjastamiskomponendi valikul midagi muud pole leitud ning jõutakse java appleti laadimiseni, siis senise 
*	appleti laadimise asemel tuleb hoiatus (vt. veakood 100)
*	- Veakoodile 100 vastava teksti detailsemaks kirjutamine
*	- Veakoodi 1500 (Java allkirjastamismoodul ei käivitunud) ära kaotamine
*
*  versioon 0.11 7. sept 2011 (Tänud RIK-ile täiendus- ja parandusettepanekute eest!)
*	- Parandatud/täiendatud pluginate laadimise tingimusi (fallback: digidocPlugin -> activeX -> javaApplet)
*	- Täiendatud java appleti laadimise ja käimamineku tuvastust
*	- Parandatud uue plugina (digidocPlugin) vigade jõudmist "üles"
*	- Hulk pisivigu parandatud
*	- lisatud veakoodid 9 ja 1500
*
*  versioon 0.10 16. mai 2011
*	- Parandatud loadSigningPlugin() meetodit
*	- Parandatud certHexToJSON() meetodit (seoses ESTEID-SK 2011 serdi lisandumisega)
*	- CodeBorne parandused keele toega seoses.
*
*  versioon 0.9  6. jaanuar 2011
*	- Parandatud plugina tuvastust, varasem versioon põstas  10.6 Safariga ccrashi

*  versioon 0.8, 29. detsember 2010
*	 - Javascripti teegi API-s muutunud: Meetod getCertificates asendatud getCertificate'ga
*	 - application/x-digidoc plugina puhul võetud kasutusele meetod getCertificate kuna getCertificates'i uutes plugina versioonides enam ei ole
*	 - Lihtsustatud ActiveX-i API kasutamist
*
*  versioon 0.7, 15. detsember 2010
*	 - Lisatud veakoodi 100 kirjeldus
*
*  versioon 0.6, 18. oktoober 2010 
*	 - Kõige esimese põlvkonna signeerimise ActiveX-i jaoks vajaliku ASN.1 struktuuri parsimisse lisatud BMPstring välja tüübi tugi
*	 - Täiustatud plugina laadimise loogikat Macil, parandatud viga mille tõttu ei laetud vanu Maci pluginaid
*
*  versioon 0.5, 8. oktoober 2010 
*	- Lisatud 2010 aastal levitatava ID-kaardi baastarkvara tugi
*	- knownCAList toodud globaalseks konfiguratsiooniparameetriks
*	- puhastatud kood mittevajalikest "debug" fragmentidest
*
*
*/


/* ------------------------------------ */
/* --- Muutujad ja andmestruktuurid --- */
/* ------------------------------------ */

var Certificate = {
    id: null,
    cert: null,
    CN: null,
    issuerCN: null,
    keyUsage: null,
    validFrom: "", // Sertifikaadi kehtivuse algusaeg, esitatud kujul dd.mm.yyyy hh:mm:ss, Zulu ajavööndis
    validTo: null // Sertifikaadi kehtivuse lõpuaeg, esitatud kujul dd.mm.yyyy hh:mm:ss, Zulu ajavööndis
}

var getCertificatesResponse = {
    certificates: [],
    returnCode: 0
}

var SignResponse = {
    signature: null,
    returnCode: 0
}

//1..99 on pluginatest tulevad vead
//ver 0.12 - veakoodi 100 detailsem kirjeldus
var dictionary = {
    1:	{est: 'Allkirjastamine katkestati',			eng: 'Signing was cancelled',			lit: 'Pasirašymas nutrauktas',					rus: 'Подпись была отменена'},
    2:	{est: 'Sertifikaate ei leitud',				eng: 'Certificate not found',			lit: 'Nerastas sertifikatas',					rus: 'Сертификат не найден'},
	9:  {est: 'Vale allkirjastamise PIN',			eng: 'Incorrect PIN code',				lit:'Incorrect PIN code',						rus: 'Неверный ПИН-код'},
    12: {est: 'ID-kaardi lugemine ebaõnnestus',		eng: 'Unable to read ID-Card',			lit: 'Nepavyko perskaityti ID-kortos',			rus: 'Невозможно считать ИД-карту'},
	14: {est: 'Tehniline viga',						eng: 'Technical error',					lit: 'Techninė klaida',							rus: 'Техническая ошибка'},
	15: {est: 'Vajalik tarkvara on puudu',			eng: 'Unable to find software',			lit: 'Nerasta programinės įranga',				rus: 'Отсутствует необходимое программное обеспечение'},
	16: {est: 'Vigane sertifikaadi identifikaator', eng: 'Invalid certificate identifier',	lit: 'Neteisingas sertifikato identifikatorius',rus: 'Неверный идентификатор сертификата'},
	17: {est: 'Vigane räsi',						eng: 'Invalid hash',					lit: 'Neteisinga santrauka',					rus: 'Неверный хеш'},
	19: {est: 'Veebis allkirjastamise käivitamine on võimalik vaid https aadressilt',		eng: 'Web signing is allowed only from https:// URL',					lit: 'Web signing is allowed only from https:// URL',					rus: 'Подпись в интернете возможна только с URL-ов, начинающихся с https://'},
	100: {est: 'Teie arvutist puudub allkirjastamistarkvara või ei ole Teie operatsioonisüsteemi ja brauseri korral veebis allkirjastamine toetatud. Allkirjastamistarkvara saate aadressilt https://installer.id.ee',		eng: 'Web signing module is missing from your computer or web signing is not supported on your operating system and browser platform. Signing software is available from https://installer.id.ee',		lit: 'Web signing module is missing from your computer or web signing is not supported on your operating system and browser platform. Signing software is available from https://installer.id.ee',				rus: 'На вашем компьютере отстутствует модуль для цифровой подписи в интернете или цифровая подпись в интернете не поддерживается вашей операционной системой и/или браузером. Программное обеспечение доступно здесь: https://installer.id.ee'}
}


var loadedPlugin = '';

// Exception

function IdCardException(returnCode, message) {
    this.returnCode = returnCode;

    this.message = message;

    this.isError = function () {
        return this.returnCode != 1;
    }

    this.isCancelled = function () {
        return this.returnCode == 1;
    }
}

//Ahto, 2013.05, See ei toimi IE puhul, põhiliselt on seda vaja mac+safari juhu jaoks.
function isPluginSupported(pluginName) {
       if (navigator.mimeTypes && navigator.mimeTypes.length) {
	       if (navigator.mimeTypes[pluginName]) {
		       return true;
	       } else {
		       return false;
	       }
       } else {
	       return false;
       }
}

function checkIfPluginIsLoaded(pluginName, lang)
{
	var plugin = document.getElementById('IdCardSigning');

	if (pluginName == "digidocPlugin")
	{
		try
		{
			var ver = plugin.version;	// Uue plugina tuvastus - uuel pluginal pole getVersion() meetodit
							// IE-s ei tule siin exceptionit, lihtsalt ver == undefined
			if (ver!==undefined) {
				return true;
			}
		}
		catch (ex)
		{
		}

		return false;
	}
	else
	{
		//Muud juhud ehk siis pluginName == "" vms
		return false;
	}
}


function getLoadedPlugin(){
	return loadedPlugin;
}

function loadSigningPlugin(lang, pluginToLoad){

	var pluginHTML = {	
		digidocPlugin:	'<object id="IdCardSigning" type="application/x-digidoc" style="width: 1px; height: 1px; visibility: hidden;"></object>'
	}
	var plugin;

	if (!lang || lang == undefined)
	{
		lang = 'eng';
	}

	//2011.05.10, ahto - juba plugina laadimisel uuritakse, kas tuldi https pealt.
	if (document.location.href.indexOf("https://") == -1)
	{
		throw new IdCardException(19, dictionary[19][lang]);
	}

	// Kontrollime, kas soovitakse laadida spetsiifiline plugin
	if (pluginToLoad != undefined)
	{
		if (pluginHTML[pluginToLoad] != undefined) // Määratud nimega plugin on olemas
		{
			document.getElementById('pluginLocation').innerHTML = pluginHTML[pluginToLoad];

			if (!checkIfPluginIsLoaded(pluginToLoad, lang))
			{
				throw new IdCardException(100, dictionary[100][lang]);
			}
			
			loadedPlugin = pluginToLoad;
		}
		else // Plugina nimi on tundmatu
		{
			// Tagastame vea juhtimaks teegi kasutaja tähelepanu valele nimele.
			throw new IdCardException(100, dictionary[100][lang]);			
		}
		return;
	} else {
		
		// 2011.05, Ahto kommentaar if lause kohta:
		// Mac+Safari juhul käivitub isPluginSupported, mis vaatab, kas plugin on arvutis olemas või mitte.
		// Teiste OS+Brauseri kombinatsioonide puhul võib lihtsalt uut pluginat laadima minna, aga Mac+Safari
		// puhul, kui püüda uut pluginat ilma selle olemasolu kontrollita laadida, näidatakse kasutajale
		// kole viga, kui pluginat pole. 
		if (
				(!(navigator.userAgent.indexOf('Mac') != -1 && navigator.userAgent.indexOf('Safari') != -1)) ||
				isPluginSupported('application/x-digidoc')
			)
		{
			document.getElementById('pluginLocation').innerHTML = pluginHTML['digidocPlugin'];
			if (checkIfPluginIsLoaded('digidocPlugin', lang))
			{
				loadedPlugin = "digidocPlugin";
				return;
			}
		}
		
		//pluginat ei suudetud laadida, anname vea
		if (loadedPlugin===undefined || loadedPlugin=="")
		{
			throw new IdCardException(100, dictionary[100][lang]);
		}
	}
}

function getISO6391Language(lang)
{
    var languageMap = {est: 'et', eng: 'en', rus: 'ru', et: 'et', en: 'en', ru: 'ru'};
    return languageMap[lang];
}

function digidocPluginHandler(lang)
{
	var plugin = document.getElementById('IdCardSigning');

    plugin.pluginLanguage = getISO6391Language(lang);

	this.getCertificate = function () {
		var TempCert;
		var response;
		var tmpErrorMessage;

		try
		{
			TempCert = plugin.getCertificate();
		}
		catch (ex)
		{
			
		}

		//2011.08.12, Ahto, saadame vea ülesse
		if (plugin.errorCode != "0")
		{
			 
			try
			{
				tmpErrorMessage = dictionary[plugin.errorCode][lang];	//exception tuleb, kui array elementi ei eksisteeri
			}
			catch (ex)
			{
				tmpErrorMessage = plugin.errorMessage;
			}

			throw new IdCardException(parseInt(plugin.errorCode), tmpErrorMessage);
		}

		// IE plugin ei tagastanud cert väljal sertifikaati HEX kujul, mistõttu on siia tehtud hack, et sertifikaadi hex kuju võetakse certificateAsHex väljalt
		if ((TempCert.cert==undefined)){
				response = '({' +
			   '    id: "' + TempCert.id + '",' +
			   '    cert: "'+TempCert.certificateAsHex+'",' +
			   '    CN: "' + TempCert.CN + '",' +
			   '    issuerCN: "' + TempCert.issuerCN + '",' +
			   '    keyUsage: "Non-Repudiation"' +
//				   '    validFrom: ' + TempCert.validFrom + ',' +
//				   '    validTo: ' + TempCert.validTo +
			   '})';
				response = eval('' + response);
				return response;
		} else {
			return TempCert;
		}
	}

	this.sign = function (id, hash ) {
		var response;
		var tmpErrorMessage;

		try
		{
			response = plugin.sign(id, hash, "");	
		}
		catch (ex)
		{}

		//2011.08.12, Ahto, saadame vea ülesse
		if (plugin.errorCode != "0")
		{
			 
			try
			{
				tmpErrorMessage = dictionary[plugin.errorCode][lang];	//exception tuleb, kui array elementi ei eksisteeri
			}
			catch (ex)
			{
				tmpErrorMessage = plugin.errorMessage;
			}

			throw new IdCardException(parseInt(plugin.errorCode), tmpErrorMessage);
		}

		
		if (response == null || response == undefined || response == "")
		{
			response = '({' + 'signature: "",' + 'returnCode: 14' + '})';
		}
		else
		{
			response = '({' + 'signature: "' + response + '",' + 'returnCode:0' + '})'
		}

		response = eval('' + response);

		if (response.returnCode != 0) {
            throw new IdCardException(response.returnCode, dictionary[response.returnCode][lang]);
        }
        return response.signature;
	}

	this.getVersion = function () {
		return plugin.version;
	}
}

function IdCardPluginHandler(lang)
{
	var plugin = document.getElementById('IdCardSigning');
	var pluginHandler = null;
	var response = null;

	if (!lang || lang == undefined)
	{
		lang = 'eng';
	}

	this.choosePluginHandler = function () {
	    return new digidocPluginHandler(lang);
	}

	this.getCertificate = function () {

		pluginHandler = this.choosePluginHandler();
		return pluginHandler.getCertificate();	
	}

	this.sign = function (id, hash) {

		pluginHandler = this.choosePluginHandler();
		return pluginHandler.sign(id, hash);
	}

	this.getVersion = function () {

		pluginHandler = this.choosePluginHandler();
		return pluginHandler.getVersion();
	}

}

$(document).ready(function() 
{
    address = document.URL;

    $('#waiting').hide();
    $('#error').hide();
    $('#success').hide();
    $('#zhuyishixiang').hide();
    $('#icon-attention').hide();
    $('#select-enlargement').hide();
    $('#enlargement').hide();

    getProperties();

    $(".myFileUpload").change(function() //选择图片
    {
        if ($(this).val() == "") {
            return;
        }
        var arrs=$(this).val().split('\\');
        filename=arrs[arrs.length-1];
        if (filename != "") {
            $("#path").html(filename);
            type = getFileType(filename);
            checkFile(); //检查文件合法性
        }
    });
    $("#reuploadpic").click(function(){ //确认错误
        $("#error").hide();
        $("#uploadpic").show();
    });
    $("#uploadpic").click(function(){ //上传图片
        if ($(".myFileUpload").val() != "") {
            //上传函数
            uploadPic();
        }    
    });
    $("#downloadpic").click(function(){
        downloadPic();
    });
});

var picid = "";
var filename = "";
var type = "";

var maxW;
var maxH;
var allowedType;
var allowedScale;
var address;

function getProperties() {
    $('#tips').show();
    $('#zhuyishixiang').show();

    allowedType = new Array();
    allowedScale = new Array();    
    $.ajax ({
        type : "GET",
        url : address + "properties", //服务器的url
        timeout: 5000,
        success : function(json) {
            maxW = json["maxW"];
            maxH = json["maxH"];
            for (var i in json["allowedType"]) {
                allowedType.push(json["allowedType"][i]);
            }
            for (var i in json["allowedScale"]) {
                allowedScale.push(json["allowedScale"][i]);
            }
            $('#tips').html("*请上传尺寸在"+maxW+"×"+maxH+"以内的图片，当前选择的图片尺寸：无");
            $('#zhuyishixiang').html("注意事项：</br>&emsp;&emsp;1. 目前仅支持对照片及写实风格的插画进行放大；</br>&emsp;&emsp;2. 支持的文件格式："
                                    + allowedType.join('/') + "；</br>&emsp;&emsp;3. 支持的放大倍数："
                                    + allowedScale.join('/') + "。");
            $('#icon-attention').show();
            createScaleOptions();
        },
        fail : function() {
            maxW = 0;
            maxH = 0;
            $('#tips').html("*连接服务器失败，请刷新页面");
        }
    });
}

function createScaleOptions() {
    $('#select-enlargement').show();
    $('#enlargement').show();
    //第一个为默认选项
    var select = document.getElementById('enlargement');
    select.options.add(new Option(allowedScale[0], allowedScale[0], true)); 
    for (var i = 1; i < allowedScale.length; i++) {
        select.options.add(new Option(allowedScale[i], allowedScale[i], false)); 
    }
}

function getFileType(filePath){
	var startIndex = filePath.lastIndexOf(".");
	if(startIndex != -1)
		return filePath.substring(startIndex+1, filePath.length).toLowerCase();
	else return "";
}
function showError(errormsg) {
    $("#uploadpic").hide();
    $("#error").show();
    $("#errormsg").html(errormsg); 
}
function checkFile() { //检测图片类型和尺寸
    if (allowedType.indexOf(type) != -1) {
        var reader = new FileReader();
        reader.readAsDataURL($(".myFileUpload")[0].files[0]); //读取文件
        reader.onload = function (e) {
            var image = new Image();
            image.src = e.target.result; //将读取的文件加载为图片，以获得宽和高
            image.onload = function () {
                var width = this.width;  
                var height = this.height; 
                $('#tips').html("*请上传尺寸在"+maxW+"×"+maxH+"以内的图片，当前选择的图片尺寸："+width+"×"+height); //改变提示信息的内容，颜色
                if (width > maxW || height > maxH) {
                    $('#tips').css("color", "rgba(255, 78, 80, 1)"); 
                    $(".myFileUpload").val("");
                    $("#uploadpic").attr("disabled","true");
                } else {
                    $('#tips').css("color", "rgba(155, 155, 155, 1)");
                    $("#uploadpic").removeAttr("disabled");
                }
            }
        };                  
    } else {
        $('#tips').html("*错误的文件格式，仅支持上传" + allowedType.join('/') + "格式的图片");
        $('#tips').css("color", "rgba(255, 78, 80, 1)");
        $(".myFileUpload").val("");
        $("#uploadpic").attr("disabled","true");
    }  
}
function uploadPic() {
    $("#uploadpic").hide();
    $("#waiting").show();
    var formData = new FormData();
    formData.append("image", $(".myFileUpload")[0].files[0]); //向formData中添加文件
    formData.append("enlarge", $("#enlargement").val());
    $.ajax ({
        type : "POST",
        url : address + "upload", //服务器的url
        timeout: 3000000,
        data: formData,
        processData: false,
        contentType: false, //无需指定内容类型，前端会自动将data以form-data形式发送
        success : function(data) { //当图片被成功保存在服务器端时，会返回被重命名的图片名称（uuid）
            $("#waiting").hide();
            picid = data; //新的名称作为全局变量被保存下来，用于后续向服务器请求生成图像
            if (picid.length < 32) { //名称长度不合法，说明返回了错误信息，报错
                showError("服务器错误，请重传");
            } else {
                $("#success").show();
            }
        },
        fail : function(data) { //报错
            showError("服务器错误，请重传");
        }
    });
}
function downloadPic() {
    $("#downloadpic").attr("disabled","true");
    var url = address + "download/" + picid; //生成GET请求的url
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);  
    xhr.responseType = "blob"; //返回值为图像，故将type设置为blob
    xhr.onload = function () {                
        if (this.status === 200) {
            var blob = new Blob([this.response], {type:"image/" + type});
            var a = document.createElement('a');
            a.download = "photo4x_" + filename;
            a.href = URL.createObjectURL(blob);
            $("#body").append(a);
            a.click();
            $(a).remove();
            $("#downloadpic").removeAttr("disabled");               
            $("#success").hide();
            $("#uploadpic").show();
        } else {
            $("#success").hide();
            showError("获取图像失败，请重传");
        }
    };
    xhr.send();
}



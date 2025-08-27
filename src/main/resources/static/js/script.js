console.log("This is script file");

const toggleSidebar = () =>{
if ($(".sidebar").is(":visible")){
    //band karna hai
    
    $(".sidebar").css("display", "none");
    $(".content").css("margin-left","0%");
}else{
    //show karna hai sidebar
     $(".sidebar").css("display", "block");
    $(".content").css("margin-left","20%");

}
};

let timer;
const search = () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
        let query = $("#search-input").val();
        if (query == "") {
            $(".search-result").hide();
        } else {
            let url = `http://localhost:8080/search/${query}`;
            fetch(url)
                .then((response) => response.json())
                .then((data) => {
                    let text = `<div class='list-group'>`;
                    data.forEach((contact) => {
                        text += `<a href='/user/${contact.cId}/contact' class='list-group-item list-group-item-action'> ${contact.name} </a>`;
                    });
                    text += `</div>`;
                    $(".search-result").html(text);
                    $(".search-result").show();
                });
        }
    }, 300); // wait 300ms after typing
};

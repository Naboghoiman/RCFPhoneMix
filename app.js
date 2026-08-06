let sliders = document.querySelectorAll("input[type=range]");

sliders.forEach(slider => {

    slider.addEventListener("input", function(){

        console.log("EQ value:", this.value);

    });

});

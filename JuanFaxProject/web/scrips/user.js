const expandingCards = document.querySelectorAll(".expanding-card")


expandingCards.forEach((card)=>{
    card.addEventListener("click", ()=>{
        const isExpanded = card.classList.toggle("cardExpandida");

        if (isExpanded){
            for (let i = 0; i < 5; i++) {
                const ExpandingTarget = document.createElement("div");
                ExpandingTarget.classList.toggle("targets-expanding");
                card.appendChild(ExpandingTarget);
                
            }
        }else{
            for (let i = 0; i < 5; i++) {
                const removeTarget=card.querySelector(".targets-expanding")
                if (removeTarget) {
                    removeTarget.remove();
                }
            }
        }
    });
});

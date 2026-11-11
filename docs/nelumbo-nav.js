// reveal.js plugin (mkslides.yml): shift-click on the previous/next arrow jumps to the first/last slide
window.NelumboNav = {
  id: 'nelumbo-nav',
  init: function (deck) {
    // capture phase: runs before reveal's own click handler on the arrow, which would step one slide
    deck.getRevealElement().addEventListener('click', function (e) {
      if (!e.shiftKey) {
        return;
      }
      var prev = e.target.closest('.controls .navigate-left');
      var next = e.target.closest('.controls .navigate-right');
      if (!prev && !next) {
        return;
      }
      e.stopPropagation();
      e.preventDefault();
      deck.slide(prev ? 0 : deck.getHorizontalSlides().length - 1);
    }, true);
  }
};

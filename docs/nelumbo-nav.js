//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

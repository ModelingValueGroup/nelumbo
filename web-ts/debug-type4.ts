import { KnowledgeBase } from './src/KnowledgeBase';
import { Tokenizer } from './src/syntax/Tokenizer';
import { Parser } from './src/syntax/Parser';
import { Type, DEFAULT_GROUP } from './src/Type';
import { Map } from 'immutable';
import { createParseContext } from './src/syntax/ParseContext';
import { PatternResult } from './src/syntax/PatternResult';

const kb = new KnowledgeBase(KnowledgeBase.BASE);
kb.run(() => {
  const text = "Type E\n";
  const tokenizer = new Tokenizer(text, "test");
  const tokResult = tokenizer.tokenize();
  
  const parser = new Parser(KnowledgeBase.CURRENT, tokResult);
  
  const typeToken = tokResult.first!.next!; // Skip BEGINOFFILE
  console.log("Token:", typeToken.text, "type:", typeToken.type.name);
  
  const ctx = createParseContext(null, null, DEFAULT_GROUP, Number.MIN_SAFE_INTEGER, null);
  
  // Try preParse with left=null for "Type" token
  const preResult = parser.preParse(typeToken, ctx, null);
  console.log("preParse result:", preResult !== null);
  
  if (preResult) {
    const left = preResult.postParse(ctx);
    console.log("postParse left:", left?.toString(), "type:", left?.type()?.name());
    
    if (left) {
      const nextTok = left.nextToken();
      console.log("Next token after Type:", nextTok?.text, nextTok?.type?.name);
      
      // Try postfix preParse with left=Type node
      const postResult = parser.preParse(nextTok, ctx, left);
      console.log("postfix preParse result:", postResult !== null);
      
      if (postResult === null) {
        // Debug: check the post patterns directly
        const postPatterns = (KnowledgeBase.CURRENT as any)._localPostPatterns;
        const state = postPatterns.get(DEFAULT_GROUP);
        if (state) {
          console.log("\nleft.type():", left.type().name());
          console.log("left.type() allSupers:", left.type().allSupers().map((t: Type) => t.name()).toArray());
          
          for (const sup of left.type().allSupers()) {
            const found = state.transitions().get(sup);
            console.log(`  get(${sup.name()}) => ${found !== undefined ? 'FOUND' : 'not found'}, sup===Type.TYPE: ${sup === Type.TYPE}`);
            if (found) {
              const testResult = new PatternResult(parser, ctx);
              testResult.left(left);
              const ok = found.parse(nextTok, testResult, Map(), true);
              console.log(`  parse result: ${ok}`);
              if (!ok) {
                console.log(`  transitions of found state:`);
                for (const [k2, v2] of found.transitions().entries()) {
                  const k2s = k2 instanceof Type ? `Type:${(k2 as Type).name()}` : String(k2);
                  console.log(`    ${k2s}`);
                }
              }
            }
          }
        } else {
          console.log("No post state for DEFAULT_GROUP");
        }
      }
    }
  }
});

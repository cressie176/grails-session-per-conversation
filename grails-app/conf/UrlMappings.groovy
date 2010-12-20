import uk.co.acuminous.spc.ConversationException
import uk.co.acuminous.spc.ConversationNotFoundException

class UrlMappings {
    static mappings = {
      "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
      "/"(view:"/index")
	  "500"(view:'/error')
      "500"(view:'/conversation/error', exception: ConversationException)
      "500"(view:'/conversation/notfound', exception: ConversationNotFoundException)        
	}
}

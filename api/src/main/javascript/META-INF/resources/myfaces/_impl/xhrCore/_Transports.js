/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * The xhr core adapter
 * which provides the transport mechanisms to the calling
 * objects, and controls the queue behavior, the error handling
 * and partial page submit functionality among other things
 *
 * The idea behind this is to make the ajax request object as barebones
 * as possible and shift the extra functionality like queuing
 * parameter handling etc... to this class so that our transports become more easily
 * pluggable. This should keep the path open to iframe submits and other transport layers
 *
 * the call to the corresponding transport just should be a
 * transport.xhrQueuedPost
 *
 * or transport.xhrPost,transport.xhrGet  etc... in the future
 *
 * Note we have taken a pattern lesson or two from the dojo toolkit and its excellent handling
 * of transports by our patterns here (which is mainly a centralized transport singleton which routes
 * to different transport implementations and the auto passing of parameters into their
 * corresponding protected attributes on class level in the transports themselves)
 *
 */
/** @namespace myfaces._impl.xhrCore._Transports */
myfaces._impl.core._Runtime.extendClass("myfaces._impl.xhrCore._Transports"
        , Object, {

    _PAR_ERRORLEVEL:"errorlevel",
    _PAR_QUEUESIZE:"queuesize",
    _PAR_PPS:"pps",
    _PAR_TIMEOUT:"timeout",
    _PAR_DELAY:"delay",


    /**
     * a singleton queue
     * note the structure of our inheritance
     * is that that _queue is attached to prototype
     * and hence the pointer to the request qeue
     * is shared over all instances
     *
     * if you need to have it per instance for complex objects
     * you have to initialize in the constructor
     *
     * (This is the same limitation dojo class inheritance
     * where our inheritance pattern is derived from has)
     */
    _q: new myfaces._impl.xhrCore._AjaxRequestQueue(),



    _Lang :  myfaces._impl._util._Lang,
    _RT: myfaces._impl.core._Runtime,


    /**
     * a simple not enqueued xhr post
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrPost : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
         delete args.xhrQueue;
        (new (this._getAjaxReqClass(context))(args)).send();
    },

    /**
     * xhr post with enqueuing as defined by the jsf 2.0 specification
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrQueuedPost : function(source, sourceForm, context, passThrgh) {
        this._q.enqueue(
                new (this._getAjaxReqClass(context))(this._getArguments(source, sourceForm, context, passThrgh)));
    },

    /**
     * xhr get without enqueuing
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrGet : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        args.ajaxType = "GET";
        delete args.xhrQueue;
        (new (this._getAjaxReqClass(context))(args)).send();
    },

    /**
     * xhr get which takes the existing queue into consideration to by synchronized
     * to previous queued post requests
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrQueuedGet : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        args.ajaxType = "GET";
        this._q.enqueue(
                new (this._getAjaxReqClass(context))(args));
    },


    /**
     * iframe post without queueing
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartPost : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        delete args.xhrQueue;
        (new (this._getMultipartReqClass(context))(args)).send();
    },

    /**
     * iframe queued post
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartQueuedPost : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        this._q.enqueue(
                new (this._getMultipartReqClass(context))(args));
    },


    /**
     * iframe get without queueing
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartGet : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        args.ajaxType = "GET";
        delete args.xhrQueue;
        (new (this._getMultipartReqClass(context))(args)).send();
    },

    /**
     * iframe queued http get
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartQueuedGet : function(source, sourceForm, context, passThrgh) {
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        args.ajaxType = "GET";
        this._q.enqueue(
                new (this._getMultipartReqClass(context))(args));
    },


    /**
     * Spec. 13.3.3
     * Examining the response markup and updating the DOM tree
     * @param {XmlHttpRequest} request - the ajax request
     * @param {XmlHttpRequest} context - the ajax context
     */
    response : function(request, context) {
        var internalContext = context._mfInternal;
        var ajaxObj = (internalContext && internalContext._mfRequest) ||  new (this._getAjaxReqClass(context))({xhr: request, context: context});
        //ie gc fix
        if(internalContext && internalContext._mfRequest){
            internalContext._mfRequest = null;
            delete internalContext._mfRequest;
        }

        ajaxObj._response.processResponse(request, context);
    },

    /**
     * creates the arguments map and
     * fetches the config params in a proper way in to
     * deal with them in a flat way (from the nested context way)
     *
     * @param source the source of the request
     * @param sourceForm the sourceform
     * @param context   the context holding all values
     * @param passThrgh the passThrough values to be blended into the response
     */
    _getArguments: function(source, sourceForm, context, passThrgh) {
        var _RT = myfaces._impl.core._Runtime;
        var _getConfig = _RT.getLocalOrGlobalConfig;
        var _Lang = myfaces._impl._util._Lang;

        var ret = {
            "source": source,
            "sourceForm": sourceForm,
            "context": context,
            "passThrough": passThrgh,
            "xhrQueue": this._q
       };

        //we now mix in the config settings which might either be set globally
        //or pushed in under the context myfaces.<contextValue> into the current request 
        this._applyConfig(ret, context, "alarmThreshold", this._PAR_ERRORLEVEL);
        this._applyConfig(ret, context, "queueSize", this._PAR_QUEUESIZE);
        this._applyConfig(ret, context, "timeout", this._PAR_TIMEOUT);
        this._applyConfig(ret, context, "delay", this._PAR_DELAY);

        //now partial page submit needs a different treatment
        //since pps == execute strings
        if (_getConfig(context, this._PAR_PPS, false)
                && _Lang.exists(passThrgh, myfaces._impl.core.Impl.P_EXECUTE)
                && passThrgh[myfaces._impl.core.Impl.P_EXECUTE].length > 0) {
            ret['partialIdsArray'] = passThrgh[myfaces._impl.core.Impl.P_EXECUTE].split(" ");
        }
        return ret;
    },

    /**
     * helper method to apply a config setting to our varargs param list
     *
     * @param destination the destination map to receive the setting
     * @param context the current context
     * @param destParm the destination param of the destination map
     * @param srcParm the source param which is the key to our config setting
     */
    _applyConfig: function(destination, context, destParm, srcParm) {
        var _RT = myfaces._impl.core._Runtime;
        var _getConfig = _RT.getLocalOrGlobalConfig;
        if (_getConfig(context, srcParm, null) != null) {
            destination[destParm] = _getConfig(context, srcParm, null);
        }
    },







    _loadImpl: function() {
        if (!this._Impl) {
            this._Impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
        }
        return this._Impl;
    },

    /**
     * centralized transport switching helper
     * for the multipart submit case
     *
     * @param context the context which is passed down
     */
    _getMultipartReqClass: function(context) {
        //if (this._RT.getLocalOrGlobalConfig(context, "transportAutoSelection", false) && this._RT.getXHRLvl() >= 2) {
        //    return myfaces._impl.xhrCore._AjaxRequestLevel2;
        //} else {
            return myfaces._impl.xhrCore._IFrameRequest;
        //}
    },


    _getAjaxReqClass: function(context) {
        return myfaces._impl.xhrCore._AjaxRequest;
    }

});
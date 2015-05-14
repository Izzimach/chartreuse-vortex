goog.provide('chartreuse_vortex.jsvortex');
goog.require('goog.object');

//
// javascript code for custom pixi sprites
//

//
// If object properties are access using bracket syntax (a['ack'] instead of a.ack)
// it's probably to prevent the google closure compiler from renaming/minimizing them
//

//
// rapidStage is a Stage-like component that will call update()
// on sub-components every frame. It adds in a prop 'perFrameUpdaters'
// that is basically an array of update functions to call every frame.
//
// Use this in place of the normal PIXI Stage and then create your
// sub-components to look for a 'perFrameUpdaters' prop on the stage. Your
// sub-component needs to create an update function and add it to the
// perFrameUpdaters array. Make sure you remove it later if the component
// is unmounted!
//
chartreuse_vortex.jsvortex = {}

chartreuse_vortex.jsvortex.rapidStage = React.createClass({
  componentDidMount: function() {
    var stage = this.refs.stage._displayObject;
    stage.perFrameUpdaters = [];
    
    // set up an event that calls all perFrameUpdaters every frame
    var perframefunc = function (hirestime) {
      var i=0;
      var updaters = stage.perFrameUpdaters;
      for (i=0; i < updaters.length; i++) {
	var updaterfunc = updaters[i];
	updaterfunc(this.props.currentTime);
      }
      this.setState({raqEvent: requestAnimationFrame(perframefunc)});
    }.bind(this);
    this.setState({raqEvent: requestAnimationFrame(perframefunc)});
  },
  shouldComponentUpdate: function(nextProps, nextState) {
    // ignore state changes that are just churn from updating raqEvent
    return (nextProps !== this.props);
  },
  componentWillUnmount: function() {
    // clean up any pending requestAnimationFrame requests
    if (typeof this.state.raqEvent !== 'undefined') {
      cancelAnimationFrame(this.state.raqEvent);
      this.setState({raqEvent: undefined});
    }
  },
  render: function() {
    var augmentedprops = goog.object.clone(this.props);
    augmentedprops.ref = "stage";
    return React.createElement(
      ReactPIXI.Stage,
      augmentedprops,
      this.props.children
    );
  }
});

//
// The VortexSprite is a sprite that takes as "input" some values
// for x0,y0,t0,... etc and can position the sprite correctly by extrapolating
// along the usual curve of a body with constant acceleration
//
var VortexSprite = function(texture) {
  PIXI.Sprite.call(this, texture);

  this['t0'] = 0;
  this['x0'] = 0;
  this['y0'] = 0;
  this['dx0'] = 0;
  this['dy0'] = 0;
  this['ddx0'] = 0;
  this['ddy0'] = 0;
  this['attime'] = 0;
  this['userData'] = null;
};
VortexSprite.prototype = Object.create(PIXI.Sprite.prototype);
VortexSprite.prototype.constructor = VortexSprite;
VortexSprite.prototype.reinterpolate = function(attime) {
  var dt = attime - this['t0'];
  this['attime'] = attime;
  this['x'] = this['x0'] + this['dx0'] * dt + 0.5 * this['ddx0'] * dt * dt;
  this['y'] = this['y0'] + this['dy0'] * dt + 0.5 * this['ddy0'] * dt * dt;
}

chartreuse_vortex.jsvortex.interpolatingSprite = ReactPIXI.CustomPIXIComponent(
  {
    customDisplayObject: function(props) {
      var texture = PIXI.Texture.fromImage(props.image)
      return new VortexSprite(texture);
    },

    customApplyProps: function(displayObject, oldProps, newProps) {
      //this.applyDisplayObjectProps(oldProps, newProps);
      // set the current interpolating values
      displayObject['t0'] = newProps['t0'];
      displayObject['x0'] = newProps['x0'];
      displayObject['y0'] = newProps['y0'];
      displayObject['dx0'] = newProps['dx0'];
      displayObject['dy0'] = newProps['dy0'];
      displayObject['ddx0'] = newProps['ddx0'];
      displayObject['ddy0'] = newProps['ddy0'];
      displayObject.reinterpolate(displayObject['attime']);
    },

    // add/remove this sprite to the list of items to update
    // on each render tick

    customDidAttach: function(displayObject) {
      var stageupdatelist = displayObject.stage.perFrameUpdaters;
      var updatefunction = function (time) {
	    displayObject.reinterpolate(time);
      };
      displayObject.userData = updatefunction;
      stageupdatelist.push(updatefunction);
    },

    customWillDetach: function(displayObject) {
      var stageupdatelist = displayObject.stage.perFrameUpdaters;
      var funcindex = stageupdatelist.indexOf(displayObject.userData);
      if (funcindex >= 0) {
	    stageupdatelist.splice(funcindex,1);
      }
    }
  }
);

chartreuse_vortex.jsvortex.userChildDisplayContainer = React.createClass(
  {
    displayName: "userChildDisplayContainer",

    getInitialState: function() {
      return { 'currentChildren': [] }
    },

    propTypes: {
      // this is called with old/new values of customChildren and should generate
      // an iterator that can be used to get a sequence of patch operations
      customUpdater: React.PropTypes.func.isRequired,
      // some opaque data type that gets passed into customUpdater
      customChildren: React.PropTypes.object.isRequired,
      // the component used to create or update react elements
      customComponent: React.PropTypes.func.isRequired
    },

    componentWillMount: function() {
      // props variables are accessed via bracket notation to prevent google closure from renaming them
      this.updateCustomChildren([], this.props['customChildren'], this.props['customUpdater'], this.props['customComponent']);
    },

    componentWillReceiveProps: function(nextProps) {
      // props variables are accessed via bracket notation to prevent google closure from renaming them
      this.updateCustomChildren(this.props['customChildren'], nextProps['customChildren'], nextProps['customUpdater'], this.props['customComponent']);
    },

    updateCustomChildren: function(oldChildren, nextChildren, customUpdater, customComponent) {
      if (typeof oldChildren === 'undefined') {
        oldChildren = [];
      }
      if (typeof nextChildren === 'undefined') {
        nextChildren = [];
      }
      //console.log("custom update children called");

      // the updater takes old and new 'children' and returns an iterator that
      // produces a sequence of modifications to children.
      var updaterInstance = customUpdater(oldChildren, nextChildren);
      var currentChildren = this.state['currentChildren'];

      var iterelement = updaterInstance.next();
      var itervalue = iterelement.value;
      while (!iterelement.done) {
        //console.log(itervalue);
        if  (itervalue['op'] === "update") {
          currentChildren[itervalue.index] = React.createElement(customComponent, itervalue.data);
        } else if (itervalue['op'] === "append") {
          // this should be at the end
          if (itervalue.index !== currentChildren.length) {
            console.log("Error: custom updater append should add elements at the end of the array");
          }
          currentChildren.push(React.createElement(customComponent, itervalue.data));
        } else if (itervalue['op'] === "remove") {
          if (itervalue.index >= currentChildren.length) {
            console.log("Error: tried to remove element past the end of the array");
          }
          currentChildren.splice(itervalue.index,1);
        } // else "noop"

        iterelement = updaterInstance.next();
        itervalue = iterelement.value;
      }

      this.setState({'currentChildren': currentChildren});
    },

    render: function() {
      return React.createElement(ReactPIXI.DisplayObjectContainer, {}, this.state['currentChildren']);
    }
  }
);
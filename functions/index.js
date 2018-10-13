const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();


exports.checkAndUpdateHit = functions.database.ref('/games/{gameId}/hits/{hitsId}')
    .onCreate((snapshot, context) => {
      const hit = snapshot.val();      

        const ref = admin.database().ref('/games').child(context.params.gameId).child('players')

        return ref.once('value', function(snapshot) {

          let health;
          var hitTo;
          snapshot.forEach(function(childSnapshot) {
            var childKey = childSnapshot.key;
            var childData = childSnapshot.val();

              if (childKey !== hit.hitBy) {
                   hitTo = childKey
                    
                   if (hit.type === 0) {
                    health = childData.health - 100;
                } else if (hit.type === 1) {
                     health = childData.health - 50;
                }

              }

          });

            snapshot.ref.parent.child('players').child(hitTo).child('health').set(50)

        });



    });